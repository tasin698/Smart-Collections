package com.smartcollections.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Memento class for implementing undo functionality.
 * 
 * DESIGN PATTERN: Memento Pattern
 * - Captures and externalizes an object's internal state
 * - Allows restoration to a previous state
 * - Maintains encapsulation (doesn't expose internal structure)
 * 
 * USE CASES:
 * - Undo item deletion
 * - Undo item editing
 * - Restore previous state after accidental changes
 * 
 * IMPLEMENTATION:
 * - Stores complete Item snapshot
 * - Records operation type and timestamp
 * - Managed by a Stack for chronological undo
 */
public class Memento implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Operation types
    public enum OperationType {
        CREATE,    // Item was created
        UPDATE,    // Item was edited
        DELETE     // Item was deleted
    }
    
    // Memento metadata
    private final String mementoId;
    private final OperationType operationType;
    private final LocalDateTime timestamp;
    
    // Saved state
    private final Item savedItem;
    private final String description;  // Human-readable operation description
    
    /**
     * Creates a memento capturing the current state of an item.
     * 
     * @param item The item to save
     * @param operationType The type of operation being performed
     * @param description Human-readable description
     */
    public Memento(Item item, OperationType operationType, String description) {
        this.mementoId = java.util.UUID.randomUUID().toString();
        this.savedItem = new Item(item);  // Deep copy using copy constructor
        this.operationType = operationType;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }
    
    // ==================== GETTERS ====================
    
    public String getMementoId() {
        return mementoId;
    }
    
    public OperationType getOperationType() {
        return operationType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Returns a copy of the saved item.
     * IMPORTANT: Returns a new copy to prevent external modification.
     */
    public Item getSavedItem() {
        return new Item(savedItem);
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the ID of the saved item.
     */
    public String getItemId() {
        return savedItem.getId();
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Returns a formatted string describing this memento.
     * Useful for displaying undo history to users.
     */
    public String getDisplayText() {
        String timeAgo = getTimeAgo();
        return String.format("%s: %s (%s)", 
            operationType, 
            description, 
            timeAgo);
    }
    
    /**
     * Calculates human-readable time since memento creation.
     */
    private String getTimeAgo() {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(timestamp, now).toMinutes();
        
        if (minutes < 1) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (minutes < 1440) {  // 24 hours
            long hours = minutes / 60;
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            long days = minutes / 1440;
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }
    
    @Override
    public String toString() {
        return "Memento{" +
                "operationType=" + operationType +
                ", description='" + description + '\'' +
                ", itemId='" + savedItem.getId() + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
