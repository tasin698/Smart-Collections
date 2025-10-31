# Smart Collections - Media & Notes Manager

A JavaFX desktop application for managing a personal media library (notes, PDFs, audio/video links, and text snippets) with advanced data structures, binary persistence, and recursive file import.

## 📋 Project Information

- **Course**: Advanced Java Programming
- **Java Version**: 23.0.2
- **JavaFX Version**: 24.0.2
- **Build Tool**: Maven
- **IDE**: Eclipse 2024-12

## ✨ Features Implemented

### Core Requirements
- ✅ **CRUD Operations**: Create, read, update, delete library items
- ✅ **Recursive Folder Import**: Scan directories for .txt, .md, .pdf, .mp3, .mp4
- ✅ **Tag & Keyword Indexing**: Fast case-insensitive search with ranking
- ✅ **Recently Viewed Stack**: Back navigation through viewed items
- ✅ **Undo Functionality**: Memento pattern for undoing create/update/delete
- ✅ **Task Queue**: PriorityQueue for study tasks with deadline-based priority
- ✅ **Binary/Object I/O**: Complete persistence with versioning and auto-backup
- ✅ **Media Preview**: Audio/video playback with MediaPlayer
- ✅ **Animations**: Smooth fade transitions for UI elements

### Data Structures Used

#### ArrayList vs LinkedList
- **ArrayList<Item>** for main storage: O(1) random access, better cache locality
- Most operations are reads and iterations, not frequent insertions/deletions

#### Stacks
- **Stack<String>** for Recently Viewed: LIFO for back navigation
- **Stack<Memento>** for Undo History: LIFO for undoing last action first

#### PriorityQueue
- **PriorityQueue<Task>**: Automatic priority ordering for study tasks
- **PriorityQueue<SearchResult>**: Ranked search results by relevance

#### Sets
- **HashSet<String>** for duplicate detection during import: O(1) contains check
- **Set<String>** for item tags: Prevents duplicate tags

#### Maps
- **HashMap<String, Item>**: O(1) item lookup by ID
- **HashMap<String, HashSet<String>>**: Keyword index for fast search
- **HashMap<String, Integer>**: Tag frequency for ranking

## 🏗️ Project Structure

```
SmartCollections/
├── pom.xml                              # Maven configuration
├── README.md                            # This file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/smartcollections/
│   │   │       ├── MainApp.java         # Application entry point
│   │   │       ├── model/
│   │   │       │   ├── Item.java        # Library item POJO
│   │   │       │   ├── Task.java        # Study task with priority
│   │   │       │   ├── Memento.java     # Undo memento
│   │   │       │   └── SearchResult.java # Ranked search result
│   │   │       ├── repository/
│   │   │       │   ├── LibraryData.java      # Serializable data container
│   │   │       │   └── LibraryRepository.java # Binary I/O operations
│   │   │       ├── service/
│   │   │       │   ├── LibraryService.java    # Main business logic
│   │   │       │   └── FileImportService.java # Recursive import
│   │   │       └── ui/
│   │   │           └── MainController.java # JavaFX controller
│   │   └── resources/
│   │       ├── fxml/
│   │       │   └── MainView.fxml        # UI layout
│   │       └── css/
│   │           └── styles.css           # Styling
│   └── test/
│       └── java/
│           └── com/smartcollections/    # Unit tests
└── .smartcollections/                   # Data directory (created at runtime)
    ├── library.dat                      # Persisted library data
    └── backups/                         # Automatic backups
```

## 🚀 Getting Started

### Prerequisites

- Java 23.0.2 or higher
- Maven 3.6+
- Eclipse 2024-12 (or any Java IDE)

### Building the Project

#### Using Eclipse

1. **Import Project**:
   - File → Import → Maven → Existing Maven Projects
   - Select the `SmartCollections` folder
   - Click Finish

2. **Build**:
   - Right-click on project → Run As → Maven install

3. **Run**:
   - Right-click on `MainApp.java` → Run As → Java Application
   - OR: Run As → Maven build → Goals: `javafx:run`

#### Using Command Line

```bash
# Build the project
mvn clean install

# Run the application
mvn javafx:run

# Create executable JAR
mvn package
```

## 💾 Persistence Format

### File Format
```
Magic Number: "SCLB" (4 bytes)
Version: 1 (int)
Serialized LibraryData object
```

### Auto-Backup Strategy
- Creates timestamped backup on each save
- Keeps last 5 backups automatically
- Backup restoration on corruption detection

### Data Location
- **Linux/Mac**: `~/.smartcollections/`
- **Windows**: `C:\Users\<username>\.smartcollections\`

## 🎯 Key Design Decisions

### 1. **ArrayList vs LinkedList**
Used ArrayList for main item storage because:
- O(1) random access by index
- Better cache locality (contiguous memory)
- Efficient iteration (most common operation)
- LinkedList advantages (O(1) insertion at ends) not needed

### 2. **HashMap for Indices**
Keyword and tag indices use HashMap for:
- O(1) average-case lookup
- Fast search performance
- Simple API

### 3. **PriorityQueue for Ranking**
Used for both tasks and search results:
- Automatic ordering by priority/relevance
- O(log n) insertion
- Always provides highest-priority item at head

### 4. **Stack for History**
LIFO structure perfect for:
- Recently viewed (most recent on top)
- Undo history (undo most recent action first)

### 5. **Memento Pattern**
For undo functionality:
- Captures complete item state
- Allows restoration without exposing internals
- Supports create/update/delete operations

## 🧪 Testing

### Running Tests
```bash
mvn test
```

### Test Coverage
- Persistence save/load operations
- Search and ranking algorithms
- Duplicate detection during import
- Undo functionality

## 📊 Complexity Analysis

### Time Complexities
- **Add Item**: O(log n) for indexing keywords
- **Search**: O(k + m log m) where k = matching items, m = results to rank
- **Undo**: O(1) stack pop
- **Get Next Task**: O(log n) priority queue poll
- **Duplicate Check**: O(1) hash set lookup

### Space Complexities
- **Main Storage**: O(n) for n items
- **Keyword Index**: O(k × i) where k = unique keywords, i = avg items per keyword
- **Undo Stack**: O(u) where u = max undo history (50)

## 🐛 Known Issues and Limitations

1. **Media Playback**: Only supports formats native to JavaFX MediaPlayer
2. **Large Imports**: UI may freeze during recursive scan of huge directories (consider background threading)
3. **Search**: Basic keyword matching (no fuzzy search or stemming)
4. **Undo Limit**: Only 50 operations kept in history

## 🔮 Future Enhancements

- [ ] Background threading for imports
- [ ] Fuzzy search with Levenshtein distance
- [ ] Export to various formats (CSV, JSON)
- [ ] Cloud backup integration
- [ ] Tag suggestions based on content
- [ ] Full-text search in PDFs
- [ ] Playlist support for media files

## 📝 Running the Application

### First Launch
1. Run the application
2. Click "Import..." to scan a directory
3. Select a folder containing supported files
4. Items will be automatically imported and indexed

### Basic Workflow
1. **Create Item**: Click "New" button
2. **Search**: Type keywords in search bar
3. **View Details**: Click item in list
4. **Edit**: Modify fields and click "Save"
5. **Undo**: Click "Undo" button to reverse last action
6. **Media**: Click "Play" to preview audio/video files

### Keyboard Shortcuts
- Enter in search field: Perform search
- (Add more shortcuts as needed)

## 👨‍💻 Author

**Student Name**: [Your Name]
**Student ID**: [Your ID]
**Course**: Advanced Java Programming
**Semester**: [Your Semester]

## 📄 License

This project is submitted as coursework for [University Name].

## 🙏 Acknowledgments

- JavaFX documentation
- Java Collections Framework
- Design Patterns (Gang of Four)

---

**Note**: This README provides comprehensive documentation for building, running, and understanding the Smart Collections application. For detailed code explanations, refer to inline comments in the source files.
