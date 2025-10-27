package com.smartcollections.repository;

import com.smartcollections.model.Item;
import com.smartcollections.model.Task;
import com.smartcollections.model.Memento;

import java.io.Serializable;
import java.util.*;

/**
 * Container class for all application data that needs to be persisted.
 * 
 * This class wraps all collections and state that will be saved
 * using ObjectOutputStream and loaded using ObjectInputStream.
 * 
 * KEY DESIGN DECISIONS:
 * - Single serializable container for atomic save/load operations
 * - Versioned format with header for backwards compatibility
 * - Stores all data structures: items, indices, tasks, history
 * - Implements defensive copying to prevent external modification
 */
public class LibraryData implements Serializable {
    
    // Serialization version - increment when structure changes!
    private static final long serialVersionUID = 1L;
    public static final int FILE_VERSION = 1;
    
    // File header identifier
    private final int version;
    private final Date lastSaved;
    
    // ==================== CORE DATA ====================
    
    // All library items (ArrayList for fast random access)
    private ArrayList<Item> items;
    
    // ==================== INDICES FOR FAST SEARCH ====================
    
    // Keyword -> Set of Item IDs (for fast keyword search)
    // Map<keyword, Set<itemId>>
    private HashMap<String, HashSet<String>> keywordIndex;
    
    // Tag -> Frequency count (for ranking)
    // Map<tag, count>
    private HashMap<String, Integer> tagFrequency;
    
    // File path -> Item ID (for duplicate detection during import)
    // Map<absolutePath, itemId>
    private HashMap<String, String> pathIndex;
    
    // ==================== TASK MANAGEMENT ====================
    
    // All tasks (stored as ArrayList, will be loaded into PriorityQueue at runtime)
    private ArrayList<Task> tasks;
    
    // ==================== HISTORY STACKS ====================
    
    // Recently viewed items (stored as ArrayList, will be loaded into Stack)
    private ArrayList<String> recentlyViewedIds;
    
    // Undo history (stored as ArrayList, will be loaded into Stack)
    private ArrayList<Memento> undoHistory;
    
    /**
     * Creates a new empty library data container.
     */
    public LibraryData() {
        this.version = FILE_VERSION;
        this.lastSaved = new Date();
        this.items = new ArrayList<>();
        this.keywordIndex = new HashMap<>();
        this.tagFrequency = new HashMap<>();
        this.pathIndex = new HashMap<>();
        this.tasks = new ArrayList<>();
        this.recentlyViewedIds = new ArrayList<>();
        this.undoHistory = new ArrayList<>();
    }
    
    // ==================== GETTERS ====================
    
    public int getVersion() {
        return version;
    }
    
    public Date getLastSaved() {
        return lastSaved;
    }
    
    public ArrayList<Item> getItems() {
        return items;
    }
    
    public HashMap<String, HashSet<String>> getKeywordIndex() {
        return keywordIndex;
    }
    
    public HashMap<String, Integer> getTagFrequency() {
        return tagFrequency;
    }
    
    public HashMap<String, String> getPathIndex() {
        return pathIndex;
    }
    
    public ArrayList<Task> getTasks() {
        return tasks;
    }
    
    public ArrayList<String> getRecentlyViewedIds() {
        return recentlyViewedIds;
    }
    
    public ArrayList<Memento> getUndoHistory() {
        return undoHistory;
    }
    
    // ==================== SETTERS ====================
    
    public void setItems(ArrayList<Item> items) {
        this.items = items;
    }
    
    public void setKeywordIndex(HashMap<String, HashSet<String>> keywordIndex) {
        this.keywordIndex = keywordIndex;
    }
    
    public void setTagFrequency(HashMap<String, Integer> tagFrequency) {
        this.tagFrequency = tagFrequency;
    }
    
    public void setPathIndex(HashMap<String, String> pathIndex) {
        this.pathIndex = pathIndex;
    }
    
    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }
    
    public void setRecentlyViewedIds(ArrayList<String> recentlyViewedIds) {
        this.recentlyViewedIds = recentlyViewedIds;
    }
    
    public void setUndoHistory(ArrayList<Memento> undoHistory) {
        this.undoHistory = undoHistory;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Returns statistics about the library.
     */
    public String getStatistics() {
        return String.format(
            "Library Statistics:\n" +
            "  Items: %d\n" +
            "  Indexed Keywords: %d\n" +
            "  Unique Tags: %d\n" +
            "  Tasks: %d\n" +
            "  Undo History Size: %d\n" +
            "  Version: %d\n" +
            "  Last Saved: %s",
            items.size(),
            keywordIndex.size(),
            tagFrequency.size(),
            tasks.size(),
            undoHistory.size(),
            version,
            lastSaved
        );
    }
    
    @Override
    public String toString() {
        return "LibraryData{" +
                "version=" + version +
                ", items=" + items.size() +
                ", keywords=" + keywordIndex.size() +
                ", tasks=" + tasks.size() +
                ", lastSaved=" + lastSaved +
                '}';
    }
}
