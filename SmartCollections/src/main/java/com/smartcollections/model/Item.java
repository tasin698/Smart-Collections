package com.smartcollections.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a library item in the Smart Collections system.
 * 
 * This class stores metadata about various types of media items:
 * - Notes (text snippets)
 * - PDFs
 * - Audio files (.mp3)
 * - Video files (.mp4)
 * - Other text files (.txt, .md)
 * 
 * KEY DESIGN DECISIONS:
 * - Implements Serializable for binary I/O persistence
 * - Uses UUID for unique identification (prevents duplicate issues)
 * - Immutable ID after creation (defensive programming)
 * - Uses Set<String> for tags to avoid duplicates
 * - LocalDateTime for timestamps (modern Java time API)
 */
public class Item implements Serializable, Comparable<Item> {
    
    // Serial version UID for serialization compatibility
    private static final long serialVersionUID = 1L;
    
    // Unique identifier (immutable after creation)
    private final String id;
    
    // Basic metadata
    private String title;
    private String category;
    private Set<String> tags;
    private int rating; // 1-5 scale
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    
    // Media/file information
    private String mediaUrl;      // For online resources
    private String filePath;      // For local files
    private String fileType;      // Extension: .txt, .pdf, .mp3, etc.
    
    // Optional fields
    private String description;
    private long fileSize;        // In bytes
    
    /**
     * Constructor for creating a new Item.
     * Automatically generates unique ID and sets creation timestamp.
     */
    public Item(String title, String category) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.category = category;
        this.tags = new HashSet<>();
        this.rating = 0;
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Copy constructor for creating mementos (undo functionality).
     * Used by the Memento pattern for state preservation.
     */
    public Item(Item other) {
        this.id = other.id;
        this.title = other.title;
        this.category = other.category;
        this.tags = new HashSet<>(other.tags);
        this.rating = other.rating;
        this.createdAt = other.createdAt;
        this.lastModified = other.lastModified;
        this.mediaUrl = other.mediaUrl;
        this.filePath = other.filePath;
        this.fileType = other.fileType;
        this.description = other.description;
        this.fileSize = other.fileSize;
    }
    
    // ==================== GETTERS ====================
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getCategory() {
        return category;
    }
    
    public Set<String> getTags() {
        // Return defensive copy to prevent external modification
        return new HashSet<>(tags);
    }
    
    public int getRating() {
        return rating;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public String getMediaUrl() {
        return mediaUrl;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    // ==================== SETTERS ====================
    
    public void setTitle(String title) {
        this.title = title;
        updateLastModified();
    }
    
    public void setCategory(String category) {
        this.category = category;
        updateLastModified();
    }
    
    /**
     * Sets the rating with validation (1-5 scale).
     * @throws IllegalArgumentException if rating is out of range
     */
    public void setRating(int rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        this.rating = rating;
        updateLastModified();
    }
    
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        updateLastModified();
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        updateLastModified();
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
        updateLastModified();
    }
    
    public void setDescription(String description) {
        this.description = description;
        updateLastModified();
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    // ==================== TAG MANAGEMENT ====================
    
    /**
     * Adds a tag to this item (case-insensitive, normalized to lowercase).
     * Uses Set to automatically prevent duplicates.
     */
    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            this.tags.add(tag.toLowerCase().trim());
            updateLastModified();
        }
    }
    
    /**
     * Removes a tag from this item.
     */
    public void removeTag(String tag) {
        if (tag != null) {
            this.tags.remove(tag.toLowerCase().trim());
            updateLastModified();
        }
    }
    
    /**
     * Checks if this item has a specific tag.
     */
    public boolean hasTag(String tag) {
        return tag != null && this.tags.contains(tag.toLowerCase().trim());
    }
    
    /**
     * Clears all tags.
     */
    public void clearTags() {
        this.tags.clear();
        updateLastModified();
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Updates the last modified timestamp.
     * Called automatically when any field is changed.
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Checks if this item has media content (audio/video).
     */
    public boolean hasMedia() {
        if (mediaUrl != null && !mediaUrl.isEmpty()) {
            return true;
        }
        if (filePath != null && !filePath.isEmpty()) {
            // Check fileType first
            if (isMediaFile()) {
                return true;
            }
            // Fallback: check file extension from path
            String lower = filePath.toLowerCase();
            return lower.endsWith(".mp3") || lower.endsWith(".mp4") || 
                   lower.endsWith(".wav") || lower.endsWith(".avi") ||
                   lower.endsWith(".m4a") || lower.endsWith(".aac") ||
                   lower.endsWith(".flv") || lower.endsWith(".mov") ||
                   lower.endsWith(".wmv") || lower.endsWith(".webm") ||
                   lower.endsWith(".ogg") || lower.endsWith(".mkv");
        }
        return false;
    }
    
    /**
     * Determines if the file type is a media file (audio/video).
     * Supports a wide range of audio and video formats.
     */
    private boolean isMediaFile() {
        if (fileType == null) return false;
        String type = fileType.toLowerCase();
        // Video formats
        boolean isVideo = type.equals(".mp4") || type.equals(".avi") || 
                         type.equals(".mov") || type.equals(".flv") ||
                         type.equals(".wmv") || type.equals(".webm") ||
                         type.equals(".mkv");
        // Audio formats
        boolean isAudio = type.equals(".mp3") || type.equals(".wav") ||
                         type.equals(".m4a") || type.equals(".aac") ||
                         type.equals(".ogg") || type.equals(".flac") ||
                         type.equals(".wma");
        return isVideo || isAudio;
    }
    
    /**
     * Checks if this item represents a document (PDF, text file).
     */
    public boolean isDocument() {
        if (fileType == null) return false;
        String type = fileType.toLowerCase();
        return type.equals(".pdf") || type.equals(".txt") || 
               type.equals(".md") || type.equals(".doc") || 
               type.equals(".docx");
    }
    
    // ==================== OBJECT METHODS ====================
    
    /**
     * Compares items by creation date (most recent first).
     * Used for sorting in collections.
     */
    @Override
    public int compareTo(Item other) {
        // Sort by most recent first
        return other.createdAt.compareTo(this.createdAt);
    }
    
    /**
     * Two items are equal if they have the same ID.
     * This is crucial for Set-based deduplication.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }
    
    /**
     * Hash code based on ID only.
     * Ensures proper behavior in HashSet and HashMap.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    /**
     * String representation for debugging and logging.
     */
    @Override
    public String toString() {
        return "Item{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", rating=" + rating +
                ", tags=" + tags +
                ", createdAt=" + createdAt +
                '}';
    }
}
