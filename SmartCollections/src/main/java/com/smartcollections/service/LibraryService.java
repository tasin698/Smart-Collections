package com.smartcollections.service;

import com.smartcollections.model.*;
import com.smartcollections.repository.LibraryData;
import com.smartcollections.repository.LibraryRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service class for managing the library.
 * 
 * RESPONSIBILITIES:
 * - CRUD operations for items
 * - Maintaining keyword and tag indices
 * - Search and ranking functionality
 * - Stack-based recently viewed and undo
 * - PriorityQueue-based task management
 * - Integration with persistence layer
 * 
 * DATA STRUCTURES USED (justifications):
 * - ArrayList<Item>: Fast random access, iteration O(1), good cache locality
 * - HashMap<String, HashSet<String>>: O(1) keyword lookup for search
 * - HashMap<String, Integer>: O(1) tag frequency retrieval
 * - PriorityQueue<SearchResult>: O(log n) insertion, automatic ranking
 * - Stack<String>: O(1) push/pop for recently viewed
 * - Stack<Memento>: O(1) push/pop for undo operations
 * - PriorityQueue<Task>: O(log n) priority-based task retrieval
 */
public class LibraryService {
    
    // Repository for persistence
    private final LibraryRepository repository;
    
    // ==================== CORE DATA (using ArrayList for fast access) ====================
    
    // Main item storage - ArrayList chosen for:
    // - O(1) random access by index
    // - Efficient iteration
    // - Better cache locality than LinkedList
    // - Most operations are reads, not frequent insertions/deletions at arbitrary positions
    private ArrayList<Item> items;
    
    // Item ID -> Item mapping for O(1) lookup
    private HashMap<String, Item> itemMap;
    
    // ==================== INDICES FOR FAST SEARCH ====================
    
    // Keyword -> Set of Item IDs
    // Enables O(1) keyword lookup, returns all items containing that keyword
    private HashMap<String, HashSet<String>> keywordIndex;
    
    // Tag -> Frequency count
    // Used for ranking search results by tag popularity
    private HashMap<String, Integer> tagFrequency;
    
    // File path -> Item ID (for duplicate detection)
    private HashMap<String, String> pathIndex;
    
    // ==================== TASK MANAGEMENT ====================
    
    // PriorityQueue for tasks - automatically maintains priority ordering
    // Higher priority tasks are automatically at the head
    private PriorityQueue<Task> taskQueue;
    
    // Task ID -> Task mapping for O(1) lookup
    private HashMap<String, Task> taskMap;
    
    // ==================== HISTORY STACKS ====================
    
    // Recently viewed items - Stack for LIFO behavior
    // Most recent item is always on top
    private Stack<String> recentlyViewed;
    private static final int MAX_RECENT = 20;
    
    // Undo history - Stack of mementos
    // Most recent action is on top, can be undone first
    private Stack<Memento> undoStack;
    private static final int MAX_UNDO = 50;
    
    /**
     * Creates a new library service and loads existing data.
     */
    public LibraryService() {
        this.repository = new LibraryRepository();
        this.items = new ArrayList<>();
        this.itemMap = new HashMap<>();
        this.keywordIndex = new HashMap<>();
        this.tagFrequency = new HashMap<>();
        this.pathIndex = new HashMap<>();
        this.taskQueue = new PriorityQueue<>();
        this.taskMap = new HashMap<>();
        this.recentlyViewed = new Stack<>();
        this.undoStack = new Stack<>();
        
        // Load existing data if available
        loadData();
    }
    
    // ==================== ITEM CRUD OPERATIONS ====================
    
    /**
     * Adds a new item to the library.
     * Updates all indices and creates a memento for undo.
     */
    public void addItem(Item item) {
        // Add to main storage
        items.add(item);
        itemMap.put(item.getId(), item);
        
        // Update indices
        indexItem(item);
        
        // Create memento for undo
        Memento memento = new Memento(item, Memento.OperationType.CREATE,
            "Created: " + item.getTitle());
        pushUndo(memento);
        
        System.out.println("Added item: " + item.getTitle());
    }
    
    /**
     * Updates an existing item.
     * Re-indexes and creates undo memento.
     */
    public void updateItem(Item item) {
        Item existing = itemMap.get(item.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Item not found: " + item.getId());
        }
        
        // Create memento BEFORE updating
        Memento memento = new Memento(existing, Memento.OperationType.UPDATE,
            "Updated: " + existing.getTitle());
        pushUndo(memento);
        
        // Remove old indices
        deindexItem(existing);
        
        // Update the item in the list
        int index = items.indexOf(existing);
        items.set(index, item);
        itemMap.put(item.getId(), item);
        
        // Re-index with new data
        indexItem(item);
        
        System.out.println("Updated item: " + item.getTitle());
    }
    
    /**
     * Deletes an item from the library.
     * Creates memento for undo capability.
     */
    public void deleteItem(String itemId) {
        Item item = itemMap.get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }
        
        // Create memento BEFORE deleting
        Memento memento = new Memento(item, Memento.OperationType.DELETE,
            "Deleted: " + item.getTitle());
        pushUndo(memento);
        
        // Remove from all structures
        items.remove(item);
        itemMap.remove(itemId);
        deindexItem(item);
        
        // Remove from recently viewed if present
        recentlyViewed.remove(itemId);
        
        System.out.println("Deleted item: " + item.getTitle());
    }
    
    /**
     * Gets an item by ID and marks it as recently viewed.
     */
    public Item getItem(String itemId) {
        Item item = itemMap.get(itemId);
        if (item != null) {
            markAsViewed(itemId);
        }
        return item;
    }
    
    /**
     * Gets all items.
     */
    public List<Item> getAllItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Gets items by category.
     */
    public List<Item> getItemsByCategory(String category) {
        return items.stream()
            .filter(item -> category.equalsIgnoreCase(item.getCategory()))
            .collect(Collectors.toList());
    }
    
    // ==================== INDEXING ====================
    
    /**
     * Indexes an item for fast keyword and tag searching.
     * 
     * Extracts keywords from:
     * - Title (split into words)
     * - Tags
     * - Description
     * 
     * All keywords are normalized to lowercase for case-insensitive search.
     */
    private void indexItem(Item item) {
        String itemId = item.getId();
        
        // Index title words
        if (item.getTitle() != null) {
            String[] words = item.getTitle().toLowerCase().split("\\s+");
            for (String word : words) {
                String cleaned = word.replaceAll("[^a-z0-9]", "");
                if (!cleaned.isEmpty()) {
                    keywordIndex.computeIfAbsent(cleaned, k -> new HashSet<>()).add(itemId);
                }
            }
        }
        
        // Index tags and update frequency
        for (String tag : item.getTags()) {
            String normalized = tag.toLowerCase();
            
            // Add to keyword index
            keywordIndex.computeIfAbsent(normalized, k -> new HashSet<>()).add(itemId);
            
            // Update tag frequency
            tagFrequency.merge(normalized, 1, Integer::sum);
        }
        
        // Index description words
        if (item.getDescription() != null) {
            String[] words = item.getDescription().toLowerCase().split("\\s+");
            for (String word : words) {
                String cleaned = word.replaceAll("[^a-z0-9]", "");
                if (!cleaned.isEmpty() && cleaned.length() > 3) { // Only meaningful words
                    keywordIndex.computeIfAbsent(cleaned, k -> new HashSet<>()).add(itemId);
                }
            }
        }
        
        // Index file path for duplicate detection
        if (item.getFilePath() != null) {
            pathIndex.put(item.getFilePath(), itemId);
        }
    }
    
    /**
     * Removes an item from all indices.
     */
    private void deindexItem(Item item) {
        String itemId = item.getId();
        
        // Remove from keyword index
        keywordIndex.values().forEach(set -> set.remove(itemId));
        
        // Update tag frequencies
        for (String tag : item.getTags()) {
            String normalized = tag.toLowerCase();
            tagFrequency.computeIfPresent(normalized, (k, count) -> {
                int newCount = count - 1;
                return newCount > 0 ? newCount : null; // Remove if count reaches 0
            });
        }
        
        // Remove from path index
        if (item.getFilePath() != null) {
            pathIndex.remove(item.getFilePath());
        }
    }
    
    /**
     * Rebuilds all indices from scratch.
     * Useful after bulk imports or data corruption.
     */
    public void rebuildIndices() {
        keywordIndex.clear();
        tagFrequency.clear();
        pathIndex.clear();
        
        for (Item item : items) {
            indexItem(item);
        }
        
        System.out.println("Indices rebuilt successfully");
    }
    
    // ==================== SEARCH ====================
    
    /**
     * Searches for items using case-insensitive keyword matching.
     * Returns results ranked by relevance using PriorityQueue.
     * 
     * RANKING ALGORITHM:
     * - Keyword matches (how many search terms matched)
     * - Tag frequency (how popular/important the tags are)
     * - Item rating (user preference)
     * - Recency (newer items get slight boost)
     */
    public List<SearchResult> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // Split query into keywords (case-insensitive)
        String[] keywords = query.toLowerCase().trim().split("\\s+");
        
        // Find all matching items
        Map<String, Integer> itemMatchCounts = new HashMap<>();
        
        for (String keyword : keywords) {
            String cleaned = keyword.replaceAll("[^a-z0-9]", "");
            if (cleaned.isEmpty()) continue;
            
            HashSet<String> matchingIds = keywordIndex.get(cleaned);
            if (matchingIds != null) {
                for (String itemId : matchingIds) {
                    itemMatchCounts.merge(itemId, 1, Integer::sum);
                }
            }
        }
        
        // Create search results with relevance scores
        // Using PriorityQueue for automatic ranking (highest relevance first)
        PriorityQueue<SearchResult> rankedResults = new PriorityQueue<>();
        
        for (Map.Entry<String, Integer> entry : itemMatchCounts.entrySet()) {
            Item item = itemMap.get(entry.getKey());
            if (item != null) {
                int keywordMatches = entry.getValue();
                int tagFreq = calculateTagFrequency(item);
                
                SearchResult result = new SearchResult(item, tagFreq, keywordMatches);
                rankedResults.offer(result);
            }
        }
        
        // Extract results from PriorityQueue (already sorted by relevance)
        List<SearchResult> results = new ArrayList<>();
        while (!rankedResults.isEmpty()) {
            results.add(rankedResults.poll());
        }
        
        System.out.println("Search for '" + query + "' returned " + results.size() + " results");
        return results;
    }
    
    /**
     * Calculates total tag frequency for an item.
     */
    private int calculateTagFrequency(Item item) {
        int total = 0;
        for (String tag : item.getTags()) {
            total += tagFrequency.getOrDefault(tag.toLowerCase(), 0);
        }
        return total;
    }
    
    /**
     * Gets items by tag.
     */
    public List<Item> getItemsByTag(String tag) {
        String normalized = tag.toLowerCase();
        HashSet<String> itemIds = keywordIndex.get(normalized);
        
        if (itemIds == null) {
            return Collections.emptyList();
        }
        
        return itemIds.stream()
            .map(itemMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    // ==================== TASK MANAGEMENT (PriorityQueue) ====================
    
    /**
     * Adds a task to the priority queue.
     * Tasks are automatically ordered by priority and deadline.
     */
    public void addTask(Task task) {
        taskQueue.offer(task);
        taskMap.put(task.getTaskId(), task);
        System.out.println("Added task: " + task.getDescription());
    }
    
    /**
     * Gets the next highest-priority task without removing it.
     */
    public Task peekNextTask() {
        return taskQueue.peek();
    }
    
    /**
     * Retrieves and removes the highest-priority task.
     */
    public Task pollNextTask() {
        Task task = taskQueue.poll();
        if (task != null) {
            System.out.println("Processing task: " + task.getDescription());
        }
        return task;
    }
    
    /**
     * Gets all tasks (for display purposes).
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(taskQueue);
    }
    
    /**
     * Removes a specific task.
     */
    public void removeTask(String taskId) {
        Task task = taskMap.get(taskId);
        if (task != null) {
            taskQueue.remove(task);
            taskMap.remove(taskId);
            System.out.println("Removed task: " + task.getDescription());
        }
    }
    
    /**
     * Updates a task's priority or deadline (requires re-insertion).
     */
    public void updateTask(Task task) {
        removeTask(task.getTaskId());
        addTask(task);
    }
    
    /**
     * Gets overdue tasks.
     */
    public List<Task> getOverdueTasks() {
        return taskQueue.stream()
            .filter(Task::isOverdue)
            .collect(Collectors.toList());
    }
    
    // ==================== RECENTLY VIEWED (Stack) ====================
    
    /**
     * Marks an item as recently viewed.
     * Uses Stack for LIFO behavior - most recent on top.
     */
    private void markAsViewed(String itemId) {
        // Remove if already in stack (to move to top)
        recentlyViewed.remove(itemId);
        
        // Push to top
        recentlyViewed.push(itemId);
        
        // Limit stack size
        if (recentlyViewed.size() > MAX_RECENT) {
            // Remove oldest (bottom of stack)
            recentlyViewed.remove(0);
        }
    }
    
    /**
     * Gets recently viewed items (most recent first).
     */
    public List<Item> getRecentlyViewed() {
        List<Item> recent = new ArrayList<>();
        
        // Iterate from top to bottom (most recent to oldest)
        for (int i = recentlyViewed.size() - 1; i >= 0; i--) {
            String itemId = recentlyViewed.get(i);
            Item item = itemMap.get(itemId);
            if (item != null) {
                recent.add(item);
            }
        }
        
        return recent;
    }
    
    /**
     * Navigates back to previously viewed item.
     */
    public Item goBack() {
        if (recentlyViewed.size() < 2) {
            return null;  // Need at least 2 items to go back
        }
        
        // Pop current item
        recentlyViewed.pop();
        
        // Peek at previous item (don't pop it)
        String previousId = recentlyViewed.peek();
        return itemMap.get(previousId);
    }
    
    // ==================== UNDO FUNCTIONALITY (Stack) ====================
    
    /**
     * Pushes a memento onto the undo stack.
     */
    private void pushUndo(Memento memento) {
        undoStack.push(memento);
        
        // Limit stack size
        if (undoStack.size() > MAX_UNDO) {
            // Remove oldest (bottom of stack)
            undoStack.remove(0);
        }
    }
    
    /**
     * Checks if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Performs undo operation.
     * Restores the previous state from the top memento.
     */
    public void undo() {
        if (!canUndo()) {
            System.out.println("Nothing to undo");
            return;
        }
        
        Memento memento = undoStack.pop();
        Item savedItem = memento.getSavedItem();
        
        switch (memento.getOperationType()) {
            case CREATE:
                // Undo create = delete the item
                items.remove(savedItem);
                itemMap.remove(savedItem.getId());
                deindexItem(savedItem);
                System.out.println("Undone: Creation of " + savedItem.getTitle());
                break;
                
            case UPDATE:
                // Undo update = restore previous version
                Item current = itemMap.get(savedItem.getId());
                if (current != null) {
                    int index = items.indexOf(current);
                    items.set(index, savedItem);
                    itemMap.put(savedItem.getId(), savedItem);
                    deindexItem(current);
                    indexItem(savedItem);
                    System.out.println("Undone: Update of " + savedItem.getTitle());
                }
                break;
                
            case DELETE:
                // Undo delete = restore the item
                items.add(savedItem);
                itemMap.put(savedItem.getId(), savedItem);
                indexItem(savedItem);
                System.out.println("Undone: Deletion of " + savedItem.getTitle());
                break;
        }
    }
    
    /**
     * Gets undo history for display.
     */
    public List<Memento> getUndoHistory() {
        List<Memento> history = new ArrayList<>(undoStack);
        Collections.reverse(history);  // Most recent first
        return history;
    }
    
    /**
     * Clears undo history.
     */
    public void clearUndoHistory() {
        undoStack.clear();
        System.out.println("Undo history cleared");
    }
    
    // ==================== DUPLICATE DETECTION ====================
    
    /**
     * Checks if a file path already exists in the library.
     * Used during recursive import to avoid duplicates.
     */
    public boolean isDuplicate(String filePath) {
        return pathIndex.containsKey(filePath);
    }
    
    /**
     * Gets item by file path (if exists).
     */
    public Item getItemByPath(String filePath) {
        String itemId = pathIndex.get(filePath);
        return itemId != null ? itemMap.get(itemId) : null;
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Returns library statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", items.size());
        stats.put("totalKeywords", keywordIndex.size());
        stats.put("uniqueTags", tagFrequency.size());
        stats.put("totalTasks", taskQueue.size());
        stats.put("overdueTasks", getOverdueTasks().size());
        stats.put("recentlyViewedCount", recentlyViewed.size());
        stats.put("undoHistorySize", undoStack.size());
        stats.put("backupsAvailable", repository.getBackupCount());
        
        // Category breakdown
        Map<String, Long> categoryCount = items.stream()
            .collect(Collectors.groupingBy(Item::getCategory, Collectors.counting()));
        stats.put("categoryCounts", categoryCount);
        
        return stats;
    }
    
    // ==================== PERSISTENCE ====================
    
    /**
     * Saves all library data to disk.
     * Creates automatic backup before saving.
     */
    public void save() {
        try {
            LibraryData data = new LibraryData();
            
            // Copy items
            data.setItems(new ArrayList<>(items));
            
            // Copy indices
            data.setKeywordIndex(new HashMap<>(keywordIndex));
            data.setTagFrequency(new HashMap<>(tagFrequency));
            data.setPathIndex(new HashMap<>(pathIndex));
            
            // Copy tasks
            data.setTasks(new ArrayList<>(taskQueue));
            
            // Copy stacks (convert to ArrayList for serialization)
            data.setRecentlyViewedIds(new ArrayList<>(recentlyViewed));
            data.setUndoHistory(new ArrayList<>(undoStack));
            
            // Save to disk
            repository.save(data);
            
            System.out.println("Library saved successfully");
            
        } catch (IOException e) {
            System.err.println("Failed to save library: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Loads library data from disk.
     */
    private void loadData() {
        try {
            LibraryData data = repository.load();
            
            // Load items
            this.items = data.getItems();
            
            // Rebuild item map
            this.itemMap.clear();
            for (Item item : items) {
                itemMap.put(item.getId(), item);
            }
            
            // Load indices
            this.keywordIndex = data.getKeywordIndex();
            this.tagFrequency = data.getTagFrequency();
            this.pathIndex = data.getPathIndex();
            
            // Load tasks
            this.taskQueue.clear();
            this.taskMap.clear();
            for (Task task : data.getTasks()) {
                taskQueue.offer(task);
                taskMap.put(task.getTaskId(), task);
            }
            
            // Load stacks
            this.recentlyViewed = new Stack<>();
            this.recentlyViewed.addAll(data.getRecentlyViewedIds());
            
            this.undoStack = new Stack<>();
            this.undoStack.addAll(data.getUndoHistory());
            
            System.out.println("Library loaded successfully");
            System.out.println(data.getStatistics());
            
        } catch (IOException e) {
            System.err.println("Failed to load library: " + e.getMessage());
            System.out.println("Starting with empty library");
        }
    }
    
    /**
     * Auto-saves on application exit.
     * Should be called in shutdown hook or close handler.
     */
    public void autoSave() {
        System.out.println("Auto-saving library...");
        save();
    }
    
    /**
     * Creates a manual backup.
     */
    public void createBackup() {
        try {
            repository.createManualBackup();
            System.out.println("Manual backup created successfully");
        } catch (IOException e) {
            System.err.println("Failed to create backup: " + e.getMessage());
        }
    }
    
    // ==================== UTILITY ====================
    
    /**
     * Clears all library data (USE WITH CAUTION!).
     */
    public void clearAll() {
        items.clear();
        itemMap.clear();
        keywordIndex.clear();
        tagFrequency.clear();
        pathIndex.clear();
        taskQueue.clear();
        taskMap.clear();
        recentlyViewed.clear();
        undoStack.clear();
        
        System.out.println("All library data cleared");
    }
    
    /**
     * Returns the repository for advanced operations.
     */
    public LibraryRepository getRepository() {
        return repository;
    }
}
