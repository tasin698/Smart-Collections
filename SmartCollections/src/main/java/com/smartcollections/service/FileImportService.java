package com.smartcollections.service;

import com.smartcollections.model.Item;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Service for recursively importing files from directories.
 * 
 * REQUIREMENTS IMPLEMENTED:
 * - Recursive directory traversal
 * - Support for: .txt, .md, .pdf, .mp3, .mp4 files
 * - Duplicate detection using Set (by absolute path)
 * - Metadata extraction (file size, type, path)
 * 
 * ALGORITHM:
 * Uses depth-first recursive traversal to scan all subdirectories.
 * For each file:
 * 1. Check if it matches supported extensions
 * 2. Check for duplicates (using Set of absolute paths)
 * 3. Extract metadata
 * 4. Create Item and add to library
 * 
 * DATA STRUCTURES:
 * - HashSet<String> for duplicate detection: O(1) lookup
 * - ArrayList<Item> for collected items: efficient iteration
 */
public class FileImportService {
    
    // Supported file extensions
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".txt", ".md", ".pdf", ".mp3", ".mp4", ".wav", ".avi", ".doc", ".docx"
    ));
    
    // Category mappings based on file type
    private static final Map<String, String> EXTENSION_CATEGORIES = new HashMap<>();
    static {
        EXTENSION_CATEGORIES.put(".txt", "Text Document");
        EXTENSION_CATEGORIES.put(".md", "Markdown Document");
        EXTENSION_CATEGORIES.put(".pdf", "PDF Document");
        EXTENSION_CATEGORIES.put(".doc", "Word Document");
        EXTENSION_CATEGORIES.put(".docx", "Word Document");
        EXTENSION_CATEGORIES.put(".mp3", "Audio");
        EXTENSION_CATEGORIES.put(".wav", "Audio");
        EXTENSION_CATEGORIES.put(".mp4", "Video");
        EXTENSION_CATEGORIES.put(".avi", "Video");
    }
    
    // Statistics for import operation
    private int filesScanned;
    private int filesImported;
    private int duplicatesSkipped;
    private int unsupportedSkipped;
    
    /**
     * Import result summary.
     */
    public static class ImportResult {
        private final List<Item> importedItems;
        private final int filesScanned;
        private final int filesImported;
        private final int duplicatesSkipped;
        private final int unsupportedSkipped;
        private final List<String> errors;
        
        public ImportResult(List<Item> importedItems, int filesScanned, 
                          int filesImported, int duplicatesSkipped, 
                          int unsupportedSkipped, List<String> errors) {
            this.importedItems = importedItems;
            this.filesScanned = filesScanned;
            this.filesImported = filesImported;
            this.duplicatesSkipped = duplicatesSkipped;
            this.unsupportedSkipped = unsupportedSkipped;
            this.errors = errors;
        }
        
        public List<Item> getImportedItems() { return importedItems; }
        public int getFilesScanned() { return filesScanned; }
        public int getFilesImported() { return filesImported; }
        public int getDuplicatesSkipped() { return duplicatesSkipped; }
        public int getUnsupportedSkipped() { return unsupportedSkipped; }
        public List<String> getErrors() { return errors; }
        
        public String getSummary() {
            return String.format(
                "Import Summary:\n" +
                "  Files Scanned: %d\n" +
                "  Files Imported: %d\n" +
                "  Duplicates Skipped: %d\n" +
                "  Unsupported Skipped: %d\n" +
                "  Errors: %d",
                filesScanned, filesImported, duplicatesSkipped, 
                unsupportedSkipped, errors.size()
            );
        }
    }
    
    /**
     * Recursively imports files from a directory.
     * 
     * @param directoryPath Path to the directory to scan
     * @param existingPaths Set of already-imported file paths (for duplicate detection)
     * @return ImportResult with statistics and imported items
     */
    public ImportResult importDirectory(String directoryPath, Set<String> existingPaths) {
        File directory = new File(directoryPath);
        
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + directoryPath);
        }
        
        // Reset statistics
        filesScanned = 0;
        filesImported = 0;
        duplicatesSkipped = 0;
        unsupportedSkipped = 0;
        
        // Collections for results
        List<Item> importedItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>(existingPaths);
        
        System.out.println("Starting recursive import from: " + directoryPath);
        
        // Start recursive scan
        try {
            recursiveScan(directory, importedItems, seenPaths, errors);
        } catch (Exception e) {
            errors.add("Fatal error during scan: " + e.getMessage());
            e.printStackTrace();
        }
        
        ImportResult result = new ImportResult(
            importedItems, filesScanned, filesImported, 
            duplicatesSkipped, unsupportedSkipped, errors
        );
        
        System.out.println(result.getSummary());
        return result;
    }
    
    /**
     * RECURSIVE METHOD: Scans directory and all subdirectories.
     * 
     * This is the core recursive algorithm:
     * 1. List all files and directories in current directory
     * 2. For each file: process it
     * 3. For each subdirectory: recursively call this method
     * 
     * BASE CASE: When directory has no subdirectories
     * RECURSIVE CASE: Call recursiveScan on each subdirectory
     */
    private void recursiveScan(File directory, List<Item> importedItems, 
                               Set<String> seenPaths, List<String> errors) {
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;  // Empty directory or access denied
        }
        
        for (File file : files) {
            filesScanned++;
            
            if (file.isDirectory()) {
                // RECURSIVE CASE: Scan subdirectory
                System.out.println("Entering directory: " + file.getName());
                recursiveScan(file, importedItems, seenPaths, errors);
                
            } else if (file.isFile()) {
                // Process file
                try {
                    Item item = processFile(file, seenPaths);
                    if (item != null) {
                        importedItems.add(item);
                        filesImported++;
                        System.out.println("  Imported: " + file.getName());
                    }
                } catch (Exception e) {
                    errors.add("Error processing " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Alternative implementation using Java NIO Files.walkFileTree()
     * This is more efficient for large directory trees.
     */
    public ImportResult importDirectoryNIO(String directoryPath, Set<String> existingPaths) {
        Path startPath = Paths.get(directoryPath);
        
        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            throw new IllegalArgumentException("Invalid directory: " + directoryPath);
        }
        
        // Reset statistics
        filesScanned = 0;
        filesImported = 0;
        duplicatesSkipped = 0;
        unsupportedSkipped = 0;
        
        List<Item> importedItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>(existingPaths);
        
        System.out.println("Starting NIO recursive import from: " + directoryPath);
        
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    filesScanned++;
                    
                    try {
                        Item item = processFile(file.toFile(), seenPaths);
                        if (item != null) {
                            importedItems.add(item);
                            filesImported++;
                            System.out.println("  Imported: " + file.getFileName());
                        }
                    } catch (Exception e) {
                        errors.add("Error processing " + file.getFileName() + ": " + e.getMessage());
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    System.out.println("Entering directory: " + dir.getFileName());
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    errors.add("Failed to access: " + file.getFileName());
                    return FileVisitResult.CONTINUE;
                }
            });
            
        } catch (IOException e) {
            errors.add("Fatal error during NIO scan: " + e.getMessage());
            e.printStackTrace();
        }
        
        ImportResult result = new ImportResult(
            importedItems, filesScanned, filesImported, 
            duplicatesSkipped, unsupportedSkipped, errors
        );
        
        System.out.println(result.getSummary());
        return result;
    }
    
    /**
     * Processes a single file: checks validity, extracts metadata, creates Item.
     * 
     * @param file The file to process
     * @param seenPaths Set of already-seen paths (for duplicate detection)
     * @return Item if file should be imported, null otherwise
     */
    private Item processFile(File file, Set<String> seenPaths) {
        // Get absolute path for duplicate detection
        String absolutePath = file.getAbsolutePath();
        
        // Check for duplicates using Set (O(1) lookup)
        if (seenPaths.contains(absolutePath)) {
            duplicatesSkipped++;
            return null;
        }
        
        // Get file extension
        String fileName = file.getName();
        String extension = getFileExtension(fileName);
        
        // Check if extension is supported
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            unsupportedSkipped++;
            return null;
        }
        
        // Add to seen paths to prevent future duplicates
        seenPaths.add(absolutePath);
        
        // Create Item from file metadata
        Item item = createItemFromFile(file, extension);
        
        return item;
    }
    
    /**
     * Creates an Item from file metadata.
     */
    private Item createItemFromFile(File file, String extension) {
        // Create item with file name as title
        String title = file.getName();
        String category = EXTENSION_CATEGORIES.getOrDefault(extension, "Other");
        
        Item item = new Item(title, category);
        
        // Set file information
        item.setFilePath(file.getAbsolutePath());
        item.setFileType(extension);
        item.setFileSize(file.length());
        
        // Auto-generate tags based on file location
        addAutomaticTags(item, file);
        
        // Set description
        item.setDescription(String.format(
            "Imported from %s\nSize: %s",
            file.getParent(),
            formatFileSize(file.length())
        ));
        
        return item;
    }
    
    /**
     * Adds automatic tags based on file path and properties.
     */
    private void addAutomaticTags(Item item, File file) {
        // Add category as tag
        item.addTag(item.getCategory());
        
        // Add parent directory name as tag
        String parentDir = file.getParentFile().getName();
        item.addTag(parentDir);
        
        // Add file type tag
        item.addTag(item.getFileType().substring(1)); // Remove leading dot
        
        // Add "imported" tag
        item.addTag("imported");
    }
    
    /**
     * Extracts file extension from filename.
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot).toLowerCase();
        }
        return "";
    }
    
    /**
     * Formats file size in human-readable format.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Gets the set of supported file extensions.
     */
    public static Set<String> getSupportedExtensions() {
        return new HashSet<>(SUPPORTED_EXTENSIONS);
    }
}
