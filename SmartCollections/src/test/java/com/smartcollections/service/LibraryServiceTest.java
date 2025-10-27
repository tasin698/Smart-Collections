package com.smartcollections.service;

import com.smartcollections.model.Item;
import com.smartcollections.model.Task;
import com.smartcollections.model.SearchResult;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LibraryService.
 * 
 * These tests demonstrate:
 * - CRUD operations
 * - Search functionality
 * - Undo operations
 * - Task queue behavior
 * - Duplicate detection
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LibraryServiceTest {
    
    private LibraryService service;
    
    @BeforeEach
    void setUp() {
        service = new LibraryService();
        service.clearAll(); // Start with clean state
    }
    
    @Test
    @Order(1)
    @DisplayName("Test adding items to library")
    void testAddItem() {
        // Create and add item
        Item item = new Item("Java Programming", "Book");
        item.addTag("programming");
        item.addTag("java");
        item.setRating(5);
        
        service.addItem(item);
        
        // Verify item was added
        assertEquals(1, service.getAllItems().size());
        
        // Verify item can be retrieved
        Item retrieved = service.getItem(item.getId());
        assertNotNull(retrieved);
        assertEquals("Java Programming", retrieved.getTitle());
        assertEquals(5, retrieved.getRating());
    }
    
    @Test
    @Order(2)
    @DisplayName("Test searching items by keyword")
    void testSearch() {
        // Add test items
        Item item1 = new Item("Java Tutorial", "Book");
        item1.addTag("java");
        item1.addTag("programming");
        service.addItem(item1);
        
        Item item2 = new Item("Python Guide", "Book");
        item2.addTag("python");
        item2.addTag("programming");
        service.addItem(item2);
        
        Item item3 = new Item("Java Advanced", "Book");
        item3.addTag("java");
        item3.addTag("advanced");
        service.addItem(item3);
        
        // Search for "java" - should return 2 results
        List<SearchResult> results = service.search("java");
        
        assertEquals(2, results.size());
        assertTrue(results.stream()
            .allMatch(r -> r.getItem().getTitle().toLowerCase().contains("java")));
    }
    
    @Test
    @Order(3)
    @DisplayName("Test case-insensitive search")
    void testCaseInsensitiveSearch() {
        Item item = new Item("JavaScript Basics", "Book");
        item.addTag("javascript");
        service.addItem(item);
        
        // Search with different cases
        List<SearchResult> results1 = service.search("JAVASCRIPT");
        List<SearchResult> results2 = service.search("javascript");
        List<SearchResult> results3 = service.search("JavaScript");
        
        assertEquals(1, results1.size());
        assertEquals(1, results2.size());
        assertEquals(1, results3.size());
    }
    
    @Test
    @Order(4)
    @DisplayName("Test updating items")
    void testUpdateItem() {
        // Add item
        Item item = new Item("Original Title", "Book");
        service.addItem(item);
        
        // Update item
        item.setTitle("Updated Title");
        item.setRating(4);
        service.updateItem(item);
        
        // Verify update
        Item retrieved = service.getItem(item.getId());
        assertEquals("Updated Title", retrieved.getTitle());
        assertEquals(4, retrieved.getRating());
    }
    
    @Test
    @Order(5)
    @DisplayName("Test deleting items")
    void testDeleteItem() {
        // Add item
        Item item = new Item("To Delete", "Book");
        service.addItem(item);
        
        assertEquals(1, service.getAllItems().size());
        
        // Delete item
        service.deleteItem(item.getId());
        
        assertEquals(0, service.getAllItems().size());
        assertNull(service.getItem(item.getId()));
    }
    
    @Test
    @Order(6)
    @DisplayName("Test undo functionality")
    void testUndo() {
        // Add item
        Item item = new Item("Test Item", "Book");
        service.addItem(item);
        
        assertEquals(1, service.getAllItems().size());
        assertTrue(service.canUndo());
        
        // Undo the addition
        service.undo();
        
        assertEquals(0, service.getAllItems().size());
    }
    
    @Test
    @Order(7)
    @DisplayName("Test undo delete restores item")
    void testUndoDelete() {
        // Add and then delete item
        Item item = new Item("Test Item", "Book");
        service.addItem(item);
        String itemId = item.getId();
        
        service.deleteItem(itemId);
        assertNull(service.getItem(itemId));
        
        // Undo the deletion
        service.undo();
        
        Item restored = service.getItem(itemId);
        assertNotNull(restored);
        assertEquals("Test Item", restored.getTitle());
    }
    
    @Test
    @Order(8)
    @DisplayName("Test duplicate detection")
    void testDuplicateDetection() {
        // Add item with file path
        Item item = new Item("File Item", "Document");
        item.setFilePath("/path/to/file.txt");
        service.addItem(item);
        
        // Check for duplicate
        assertTrue(service.isDuplicate("/path/to/file.txt"));
        assertFalse(service.isDuplicate("/different/path.txt"));
    }
    
    @Test
    @Order(9)
    @DisplayName("Test task priority queue")
    void testTaskQueue() {
        // Add tasks with different priorities
        Item item1 = new Item("Study Item 1", "Book");
        service.addItem(item1);
        
        Task lowPriority = new Task(
            item1.getId(), 
            item1.getTitle(), 
            "Low priority task",
            Task.TaskPriority.LOW,
            java.time.LocalDateTime.now().plusDays(7)
        );
        
        Task highPriority = new Task(
            item1.getId(), 
            item1.getTitle(), 
            "High priority task",
            Task.TaskPriority.URGENT,
            java.time.LocalDateTime.now().plusHours(2)
        );
        
        // Add in wrong order
        service.addTask(lowPriority);
        service.addTask(highPriority);
        
        // High priority should come first
        Task next = service.peekNextTask();
        assertNotNull(next);
        assertEquals(Task.TaskPriority.URGENT, next.getPriority());
    }
    
    @Test
    @Order(10)
    @DisplayName("Test tag frequency tracking")
    void testTagFrequency() {
        // Add items with overlapping tags
        Item item1 = new Item("Item 1", "Book");
        item1.addTag("java");
        item1.addTag("programming");
        service.addItem(item1);
        
        Item item2 = new Item("Item 2", "Book");
        item2.addTag("java");
        item2.addTag("advanced");
        service.addItem(item2);
        
        // Search should rank items with more frequent tags higher
        List<SearchResult> results = service.search("java");
        
        assertEquals(2, results.size());
        // Both items should have "java" tag, so both should appear
    }
    
    @Test
    @Order(11)
    @DisplayName("Test recently viewed stack")
    void testRecentlyViewed() {
        // Add items
        Item item1 = new Item("Item 1", "Book");
        Item item2 = new Item("Item 2", "Book");
        Item item3 = new Item("Item 3", "Book");
        
        service.addItem(item1);
        service.addItem(item2);
        service.addItem(item3);
        
        // View items
        service.getItem(item1.getId());
        service.getItem(item2.getId());
        service.getItem(item3.getId());
        
        // Recently viewed should have all 3, with item3 most recent
        List<Item> recent = service.getRecentlyViewed();
        
        assertEquals(3, recent.size());
        assertEquals(item3.getId(), recent.get(0).getId()); // Most recent first
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        service.clearAll();
    }
}
