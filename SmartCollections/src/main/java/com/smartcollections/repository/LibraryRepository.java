package com.smartcollections.repository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Repository class for persisting and loading library data using binary/object I/O.
 * 
 * REQUIREMENTS IMPLEMENTED:
 * - Binary/Object I/O using ObjectOutputStream/ObjectInputStream
 * - Versioned file format with header validation
 * - Auto-backup functionality on save
 * - Defensive coding with try-with-resources
 * - Error handling and recovery
 * 
 * FILE FORMAT:
 * - Magic number (4 bytes): "SCLB" (Smart Collections Library)
 * - Version number (int)
 * - Serialized LibraryData object
 * 
 * BACKUP STRATEGY:
 * - Creates timestamped backups on each save
 * - Keeps last 5 backups automatically
 * - Allows manual backup creation
 */
public class LibraryRepository {
    
    // File format constants
    private static final String MAGIC_NUMBER = "SCLB";  // Smart Collections Library
    private static final String DEFAULT_FILENAME = "library.dat";
    private static final String BACKUP_SUFFIX = ".backup";
    private static final int MAX_BACKUPS = 5;
    
    // File paths
    private final Path dataDirectory;
    private final Path dataFilePath;
    private final Path backupDirectory;
    
    /**
     * Creates a repository with default data directory in user home.
     */
    public LibraryRepository() {
        this(Paths.get(System.getProperty("user.home"), ".smartcollections"));
    }
    
    /**
     * Creates a repository with custom data directory.
     */
    public LibraryRepository(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.dataFilePath = dataDirectory.resolve(DEFAULT_FILENAME);
        this.backupDirectory = dataDirectory.resolve("backups");
        
        // Ensure directories exist
        initializeDirectories();
    }
    
    /**
     * Initializes data and backup directories.
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(backupDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create data directories: " + e.getMessage());
        }
    }
    
    // ==================== SAVE OPERATIONS ====================
    
    /**
     * Saves library data with automatic backup.
     * 
     * PROCESS:
     * 1. Create backup of existing file
     * 2. Write new data to temporary file
     * 3. Validate written data
     * 4. Replace old file with new file atomically
     * 5. Clean up old backups
     * 
     * This ensures data integrity - if save fails, original file remains intact.
     */
    public void save(LibraryData data) throws IOException {
        // Step 1: Create backup of existing file
        if (Files.exists(dataFilePath)) {
            createBackup();
        }
        
        // Step 2: Write to temporary file first
        Path tempFile = dataFilePath.resolveSibling(DEFAULT_FILENAME + ".tmp");
        
        try {
            writeData(tempFile, data);
            
            // Step 3: Validate the written file
            validateFile(tempFile);
            
            // Step 4: Atomically replace old file with new file
            Files.move(tempFile, dataFilePath, StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("Library saved successfully: " + dataFilePath);
            
        } catch (IOException e) {
            // Clean up temporary file on error
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException deleteError) {
                // Ignore cleanup errors
            }
            throw new IOException("Failed to save library: " + e.getMessage(), e);
        }
        
        // Step 5: Clean up old backups
        cleanupOldBackups();
    }
    
    /**
     * Writes library data to file using ObjectOutputStream.
     * 
     * FORMAT:
     * 1. Magic number (UTF string)
     * 2. Version number (int)
     * 3. Serialized LibraryData object
     */
    private void writeData(Path filePath, LibraryData data) throws IOException {
        // Use try-with-resources for automatic resource management
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            
            // Write file header
            oos.writeUTF(MAGIC_NUMBER);
            oos.writeInt(LibraryData.FILE_VERSION);
            
            // Write serialized data
            oos.writeObject(data);
            
            // Ensure all data is written
            oos.flush();
        }
    }
    
    /**
     * Creates a timestamped backup of the current data file.
     */
    private void createBackup() throws IOException {
        if (!Files.exists(dataFilePath)) {
            return;
        }
        
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFilename = DEFAULT_FILENAME + BACKUP_SUFFIX + "_" + timestamp;
        Path backupPath = backupDirectory.resolve(backupFilename);
        
        Files.copy(dataFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backup created: " + backupPath);
    }
    
    /**
     * Auto-backup: convenience method for explicit backup requests.
     */
    public void createManualBackup() throws IOException {
        if (!Files.exists(dataFilePath)) {
            throw new IOException("No data file exists to backup");
        }
        createBackup();
    }
    
    /**
     * Removes old backups, keeping only the most recent MAX_BACKUPS.
     */
    private void cleanupOldBackups() {
        try {
            File[] backups = backupDirectory.toFile().listFiles(
                (dir, name) -> name.contains(BACKUP_SUFFIX)
            );
            
            if (backups != null && backups.length > MAX_BACKUPS) {
                // Sort by last modified date (oldest first)
                java.util.Arrays.sort(backups, 
                    java.util.Comparator.comparingLong(File::lastModified));
                
                // Delete oldest backups
                for (int i = 0; i < backups.length - MAX_BACKUPS; i++) {
                    Files.deleteIfExists(backups[i].toPath());
                    System.out.println("Deleted old backup: " + backups[i].getName());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to cleanup backups: " + e.getMessage());
        }
    }
    
    // ==================== LOAD OPERATIONS ====================
    
    /**
     * Loads library data from file.
     * 
     * Returns a new empty LibraryData if file doesn't exist.
     * Attempts to restore from backup if main file is corrupted.
     */
    public LibraryData load() throws IOException {
        // If no data file exists, return empty library
        if (!Files.exists(dataFilePath)) {
            System.out.println("No existing library found, creating new one");
            return new LibraryData();
        }
        
        try {
            return readData(dataFilePath);
        } catch (Exception e) {
            System.err.println("Failed to load main library file: " + e.getMessage());
            System.out.println("Attempting to restore from backup...");
            
            // Try to restore from most recent backup
            return loadFromBackup();
        }
    }
    
    /**
     * Reads library data from file using ObjectInputStream.
     */
    private LibraryData readData(Path filePath) throws IOException, ClassNotFoundException {
        // Use try-with-resources for automatic resource management
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            
            // Read and validate file header
            String magic = ois.readUTF();
            if (!MAGIC_NUMBER.equals(magic)) {
                throw new IOException("Invalid file format: magic number mismatch");
            }
            
            int version = ois.readInt();
            if (version > LibraryData.FILE_VERSION) {
                throw new IOException("File version " + version + 
                    " is newer than supported version " + LibraryData.FILE_VERSION);
            }
            
            // Read serialized data
            LibraryData data = (LibraryData) ois.readObject();
            
            System.out.println("Library loaded successfully: " + filePath);
            System.out.println(data.getStatistics());
            
            return data;
        }
    }
    
    /**
     * Attempts to load from the most recent backup file.
     */
    private LibraryData loadFromBackup() throws IOException {
        File[] backups = backupDirectory.toFile().listFiles(
            (dir, name) -> name.contains(BACKUP_SUFFIX)
        );
        
        if (backups == null || backups.length == 0) {
            throw new IOException("No backups available");
        }
        
        // Sort by last modified date (newest first)
        java.util.Arrays.sort(backups, 
            java.util.Comparator.comparingLong(File::lastModified).reversed());
        
        // Try each backup until one works
        for (File backup : backups) {
            try {
                System.out.println("Trying backup: " + backup.getName());
                LibraryData data = readData(backup.toPath());
                System.out.println("Successfully restored from backup!");
                
                // Restore the backup as the main file
                Files.copy(backup.toPath(), dataFilePath, 
                    StandardCopyOption.REPLACE_EXISTING);
                
                return data;
            } catch (Exception e) {
                System.err.println("Backup failed: " + e.getMessage());
                // Continue to next backup
            }
        }
        
        throw new IOException("All backup restoration attempts failed");
    }
    
    // ==================== VALIDATION ====================
    
    /**
     * Validates that a file can be read successfully.
     * Used to verify newly written files before replacing the main file.
     */
    private void validateFile(Path filePath) throws IOException {
        try {
            readData(filePath);
        } catch (Exception e) {
            throw new IOException("File validation failed: " + e.getMessage(), e);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Checks if a data file exists.
     */
    public boolean dataFileExists() {
        return Files.exists(dataFilePath);
    }
    
    /**
     * Returns the path to the data file.
     */
    public Path getDataFilePath() {
        return dataFilePath;
    }
    
    /**
     * Returns the number of available backups.
     */
    public int getBackupCount() {
        File[] backups = backupDirectory.toFile().listFiles(
            (dir, name) -> name.contains(BACKUP_SUFFIX)
        );
        return backups != null ? backups.length : 0;
    }
    
    /**
     * Deletes all data (main file and backups).
     * USE WITH CAUTION!
     */
    public void deleteAll() throws IOException {
        Files.deleteIfExists(dataFilePath);
        
        File[] backups = backupDirectory.toFile().listFiles();
        if (backups != null) {
            for (File backup : backups) {
                Files.deleteIfExists(backup.toPath());
            }
        }
        
        System.out.println("All library data deleted");
    }
}
