# Smart Collections - Media & Notes Manager

A comprehensive JavaFX desktop application for managing a personal media library including notes, PDFs, audio/video files, and text documents. Features advanced data structures, binary persistence, recursive file import, and intelligent search with ranking.

##  Project Information

- **Course**: Advanced Java Programming
- **Java Version**: 23.0.2
- **JavaFX Version**: 24.0.2
- **Build Tool**: Maven
- **IDE**: Eclipse 2024-12
- **Total Lines of Code**: ~3,680 lines
- **Architecture**: MVC Pattern with Service Layer

##  Features Implemented

###  All 9 Functional Requirements Complete

| # | Requirement | Implementation | Status |
|---|------------|----------------|--------|
| 1 | **Library CRUD** | Create, read, update, delete items with full metadata | ✅ Complete |
| 2 | **Recursive Import** | Depth-first directory scan with duplicate detection | ✅ Complete |
| 3 | **Tag & Keyword Index** | HashMap-based indices with PriorityQueue ranking | ✅ Complete |
| 4 | **Recently Viewed** | Stack-based LIFO navigation | ✅ Complete |
| 5 | **Undo Functionality** | Memento pattern with 50-operation history | ✅ Complete |
| 6 | **Task Priority Queue** | Deadline-based automatic prioritization | ✅ Complete |
| 7 | **Binary Persistence** | Versioned ObjectOutputStream with auto-backup | ✅ Complete |
| 8 | **Media Preview** | Audio/video playback with full controls | ✅ Complete |
| 9 | **Animations** | Smooth FadeTransition for UI elements | ✅ Complete |

###  Supported File Formats

#### Documents (5 formats)
- **Type**: `.txt`, `.md`, `.doc`, `.docx`, `.pdf`

#### Audio (7 formats)
- `.mp3`, `.wav`, `.m4a`, `.aac`, `.ogg`, `.flac`, `.wma`

#### Video (7 formats)
- `.mp4`, `.avi`, `.mov`, `.flv`, `.wmv`, `.webm`, `.mkv`

**Total: 19 file formats supported**

##  Architecture & Design

### MVC/MVVM Pattern

```
┌─────────────────────────────────────────────┐
│                   UI Layer                  │
│  (MainController.java, MainView.fxml)       │
│  - Event handling, user interactions        │
│  - JavaFX controls, animations              │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│                Service Layer                │
│  (LibraryService, FileImportService)        │
│  - Business logic, algorithms               │
│  - Data structure management                │
│  - Search, ranking, undo operations         │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│              Repository Layer               │
│  (LibraryRepository, LibraryData)           │
│  - Binary I/O, persistence                  │
│  - Backup management                        │
└─────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│                 Model Layer                 │
│  (Item, Task, Memento, SearchResult)        │
│  - POJOs, data representation               │
│  - Serializable entities                    │
└─────────────────────────────────────────────┘
```

### Data Structures - Strategic Choices

#### 1. ArrayList<Item> - Main Storage
```java
private ArrayList<Item> items;
```
**Why ArrayList over LinkedList?**
- ✅ O(1) random access by index
- ✅ Better cache locality (contiguous memory allocation)
- ✅ Efficient iteration (most common operation: O(n))
- ✅ Minimal memory overhead (no node pointers)
- ❌ LinkedList O(1) insertion at ends not needed (rare operation)

**Trade-off Analysis:**
```
Operation           | ArrayList | LinkedList | Our Need  | Choice
--------------------|-----------|------------|-----------|--------
Random Access       | O(1)      | O(n)       | Frequent  | ArrayList
Iteration           | O(n)      | O(n)       | Very Freq | ArrayList
Add at End          | O(1)*     | O(1)       | Rare      | Tie
Insert at Position  | O(n)      | O(1)**     | Never     | ArrayList
Memory per Element  | Low       | High       | Important | ArrayList
Cache Performance   | Excellent | Poor       | Important | ArrayList

* Amortized
** Only if position known
```

#### 2. HashMap<String, HashSet<String>> - Keyword Index
```java
private HashMap<String, HashSet<String>> keywordIndex;
// keyword -> set of item IDs containing that keyword
```
**Rationale:**
- O(1) average-case keyword lookup
- HashSet prevents duplicate item IDs
- Enables fast search: find all items with specific keyword
- Space complexity: O(k × i) where k = unique keywords, i = avg items per keyword

#### 3. HashMap<String, Integer> - Tag Frequency
```java
private HashMap<String, Integer> tagFrequency;
// tag -> count of items with that tag
```
**Purpose:**
- Track tag popularity for search ranking
- O(1) frequency retrieval
- Used in PriorityQueue comparator for ranking

#### 4. PriorityQueue<SearchResult> - Ranked Search
```java
PriorityQueue<SearchResult> rankedResults = new PriorityQueue<>(
    Comparator.comparingInt(SearchResult::getFrequency).reversed()
);
```
**Benefits:**
- Automatic ordering by frequency (highest first)
- O(log n) insertion maintains heap property
- O(1) access to highest-priority result
- Elegant API for "top k" results

#### 5. Stack<String> - Recently Viewed
```java
private Stack<String> recentlyViewed;  // Item IDs, max 20
```
**LIFO Behavior Perfect For:**
- Most recent item always on top
- "Back" navigation (pop to previous)
- O(1) push/pop operations
- Natural semantic match for "recent history"

#### 6. Stack<Memento> - Undo History
```java
private Stack<Memento> undoStack;  // Max 50 mementos
```
**Memento Pattern Implementation:**
- LIFO ensures most recent action undone first
- Each Memento captures complete item state
- O(1) undo operation (pop + restore)
- Bounded size (50) prevents memory bloat

#### 7. PriorityQueue<Task> - Task Management
```java
private PriorityQueue<Task> taskQueue;
```
**Custom Comparator:**
```java
Task implements Comparable<Task> {
    @Override
    public int compareTo(Task other) {
        int basePriority = Integer.compare(other.priority, this.priority);
        if (basePriority != 0) return basePriority;
        
        // Deadline urgency: sooner deadline = higher priority
        if (this.deadline != null && other.deadline != null) {
            return this.deadline.compareTo(other.deadline);
        }
        return 0;
    }
}
```
- Automatically maintains task ordering
- Next task always at head: O(1) access
- Insert new task: O(log n)
- Combines base priority + deadline urgency

#### 8. HashSet<String> - Duplicate Detection
```java
Set<String> seenPaths = new HashSet<>();  // During import
```
**Critical for Import:**
- O(1) contains() check for duplicate file paths
- Prevents re-importing same file
- Uses absolute path canonicalization
- Efficient with large imports (1000s of files)

### Summary: Big-O Complexity

| Operation | Complexity | Data Structure Used |
|-----------|-----------|---------------------|
| **Add Item** | O(log n) | ArrayList + HashMap indexing |
| **Get Item by ID** | O(1) | HashMap lookup |
| **Search by Keyword** | O(k + m log m)* | HashMap + PriorityQueue |
| **Undo Last Action** | O(1) | Stack pop |
| **Get Next Task** | O(log n) | PriorityQueue poll |
| **Check Duplicate** | O(1) | HashSet contains |
| **Recently Viewed** | O(1) | Stack push/pop |
| **List All Items** | O(n) | ArrayList iteration |

\* k = items matching keyword, m = results to rank

## 🏛️ Project Structure

```
SmartCollections/
├── pom.xml                                  # Maven build configuration
├── README.md                                # This file
├── .gitignore                               # Git ignore rules
├── dependency-reduced-pom.xml               # Shaded JAR config
│
├── src/main/
│   ├── java/com/smartcollections/
│   │   │
│   │   ├── MainApp.java                     # Application entry point
│   │   │                                    # - JavaFX application setup
│   │   │                                    # - Scene initialization
│   │   │
│   │   ├── model/                           # Domain models (POJOs)
│   │   │   ├── Item.java                    # Library item
│   │   │   │                                # - Serializable entity
│   │   │   │                                # - UUID-based identification
│   │   │   │                                # - Copy constructor for Memento
│   │   │   │                                # - Comparable for sorting
│   │   │   │
│   │   │   ├── Task.java                    # Study task
│   │   │   │                                # - Implements Comparable
│   │   │   │                                # - Priority + deadline logic
│   │   │   │                                # - Serializable
│   │   │   │
│   │   │   ├── Memento.java                 # Undo state snapshot
│   │   │   │                                # - Captures item state
│   │   │   │                                # - Operation type tracking
│   │   │   │                                # - Timestamp for debugging
│   │   │   │
│   │   │   └── SearchResult.java            # Ranked search result
│   │   │                                    # - Item + frequency score
│   │   │                                    # - Comparable for ranking
│   │   │
│   │   ├── repository/                      # Data persistence layer
│   │   │   ├── LibraryData.java             # Serializable container
│   │   │   │                                # - Aggregates all data
│   │   │   │                                # - Items, tasks, history
│   │   │   │                                # - Serializable
│   │   │   │
│   │   │   └── LibraryRepository.java       # Binary I/O operations
│   │   │                                    # - ObjectOutputStream/InputStream
│   │   │                                    # - Versioned file format
│   │   │                                    # - Auto-backup (5 most recent)
│   │   │                                    # - Try-with-resources
│   │   │                                    # - Error handling & recovery
│   │   │
│   │   ├── service/                         # Business logic layer
│   │   │   ├── LibraryService.java          # Main service
│   │   │   │                                # - All data structure management
│   │   │   │                                # - CRUD operations
│   │   │   │                                # - Search & ranking algorithms
│   │   │   │                                # - Undo/redo logic
│   │   │   │                                # - Task queue management
│   │   │   │                                # - Integration with repository
│   │   │   │
│   │   │   └── FileImportService.java       # Recursive import
│   │   │                                    # - Depth-first directory traversal
│   │   │                                    # - RECURSIVE algorithm
│   │   │                                    # - Duplicate detection (HashSet)
│   │   │                                    # - Metadata extraction
│   │   │                                    # - Import statistics
│   │   │
│   │   └── ui/                              # Presentation layer
│   │       └── MainController.java          # JavaFX controller
│   │                                        # - FXML bindings (@FXML)
│   │                                        # - Event handlers (lambdas)
│   │                                        # - Media playback controls
│   │                                        # - Animation logic
│   │                                        # - UI updates (Platform.runLater)
│   │
│   ├── resources/
│   │   ├── fxml/
│   │   │   └── MainView.fxml                # UI layout definition
│   │   │                                    # - BorderPane structure
│   │   │                                    # - SplitPane for 3-column layout
│   │   │                                    # - MediaView for playback
│   │   │                                    # - Controls: Button, TextField, etc.
│   │   │
│   │   └── css/
│   │       └── styles.css                   # UI styling
│   │                                        # - Professional color scheme
│   │                                        # - Media controls styling
│   │                                        # - Responsive layouts
│   │
│   └── module-info.java                     # Java module definition
│                                            # - JavaFX module requirements
│                                            # - Exports and opens
│
├── src/test/
│   └── java/com/smartcollections/
│       └── service/
│           └── LibraryServiceTest.java      # Unit tests
│                                            # - JUnit 5
│                                            # - Tests for CRUD, search, undo
│                                            # - Ranking testing
│
└── .smartcollections/                       # Runtime data directory
    ├── library.dat                          # Binary data file
    │                                        # Format: SCLB header + version + object
    └── backups/                             # Timestamped backups
        ├── library_20251027_103045.backup
        ├── library_20251027_104523.backup
        └── ...                              # (keeps last 5)
```



##  Testing

### Unit Tests (JUnit 5)

```bash
mvn test
```

**Test Coverage:**
- ✅ Item CRUD operations
- ✅ Search functionality
- ✅ Undo/redo operations
- ✅ Persistence (save/load)
- ✅ Duplicate detection
- ✅ Tag indexing

**Example Test:**
```java
@Test
void testSearchRanking() {
    Item item1 = new Item("Java Programming", "Book");
    item1.getTags().add("java");
    item1.getTags().add("programming");
    
    Item item2 = new Item("Python Guide", "Book");
    item2.getTags().add("python");
    
    service.addItem(item1);
    service.addItem(item2);
    
    List<Item> results = service.search("java");
    assertEquals(1, results.size());
    assertEquals("Java Programming", results.get(0).getTitle());
}
```

##  Build & Run

### Prerequisites
- Java 23.0.2+
- Maven 3.6+
- Eclipse 2024-12 (or IntelliJ IDEA)

### Eclipse Setup
1. File → Import → Maven → Existing Maven Projects
2. Select `SmartCollections` folder
3. Right-click project → Maven → Update Project
4. Right-click `MainApp.java` → Run As → Maven Install
5. `MainApp.java` → Run As → Java Application

### Command Line
```bash
# Build
mvn clean install

# Run
mvn javafx:run

# Test
mvn test

# Package (executable JAR)
mvn package
```

### Run Executable JAR
```bash
java -jar target/smart-collections-1.0.0-shaded.jar
```

##  Key Learning Outcomes Demonstrated

### 1. Data Structures & Algorithms
- ✅ Strategic choice of data structures (ArrayList vs LinkedList)
- ✅ Big-O complexity analysis
- ✅ Trade-off evaluation
- ✅ Recursive algorithms (directory traversal)
- ✅ Search and ranking algorithms

### 2. Object-Oriented Programming
- ✅ Encapsulation (private fields, public interfaces)
- ✅ Inheritance (extends, implements)
- ✅ Polymorphism (Comparable, Serializable)
- ✅ Composition (has-a relationships)
- ✅ Design patterns (Memento, Repository, MVC)

### 3. JavaFX & UI Development
- ✅ FXML-based UI design
- ✅ Event-driven programming
- ✅ Property bindings
- ✅ Lambda expressions
- ✅ Animations and transitions
- ✅ Media playback integration

### 4. File I/O & Persistence
- ✅ Binary I/O (ObjectOutputStream/InputStream)
- ✅ Serialization (Serializable interface)
- ✅ File format versioning
- ✅ Error handling (try-with-resources)
- ✅ Backup strategies

### 5. Software Engineering Practices
- ✅ MVC architecture
- ✅ Separation of concerns
- ✅ Defensive programming
- ✅ Unit testing (JUnit)
- ✅ Code documentation
- ✅ Maven build management

## 🐛 Known Limitations

1. **UI Responsiveness**: Large imports (1000+ files) may freeze UI
   - *Solution*: Consider background threading (Task/Service)

2. **Media Formats**: Limited to JavaFX MediaPlayer native formats
   - *Workaround*: External player integration possible

3. **Search**: Basic keyword matching, no fuzzy search

4. **Undo Depth**: Limited to 50 operations
   - *Rationale*: Memory management trade-off

5. **File Encoding**: Assumes UTF-8 for text files
   - *Improvement*: Auto-detection with charset library


##  Usage Guide

### First-Time Setup
1. Launch application
2. Data directory created at `~/.smartcollections/`
3. Empty library displayed

### Importing Files
1. Click "Import..." button
2. Select folder containing supported files
3. Recursive scan begins
4. Progress displayed in status bar
5. Items appear in library list

### Creating Items Manually
1. Click "New" button
2. Fill in title, category, tags
3. Set rating with slider
4. Add file path (optional)
5. Click "Save"

### Searching
1. Type keywords in search bar
2. Press Enter or click "Search"
3. Results ranked by relevance
4. Clear with "Clear" button

### Using Undo
1. Make changes (create/edit/delete)
2. Click "Undo" button
3. Last action reversed
4. Up to 50 operations can be undone

### Managing Tasks
1. Click "Add Task" in Tasks panel
2. Enter task details
3. Set priority (1-5)
4. Set deadline
5. Tasks auto-sorted by priority
6. Click "Process Next" to complete top task

### Media Playback
1. Select audio/video item
2. Click "Play" button
3. Use timeline slider to seek
4. Adjust volume with slider
5. Click "Fullscreen" for full-screen playback

##  Acknowledgments

- **JavaFX Documentation**: UI framework reference
- **Java Collections Framework**: Data structure implementations
- **Design Patterns (Gang of Four)**: Architectural guidance
- **Maven Documentation**: Build tool usage
- **JUnit 5 User Guide**: Testing framework

## 📄 License

This project is submitted as coursework for an Advanced Java Programming course.

---

**Author**: Tasin Towsif Rahman  
**Student ID**: S00391967 
**Course**: Advanced Java Programming  
**Institution**: Australin Catholic University  
**Date**: 30 October 2025

---

##  Support

For issues or questions about this project:
1. Check inline code comments
2. Review this README
3. Consult JavaFX documentation


---

**End of README**

*This README provides comprehensive technical documentation for the Smart Collections application. All code is well-commented and follows Java best practices.*
