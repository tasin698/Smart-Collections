# Quick Start Guide - Smart Collections

This guide will get you up and running in 5 minutes!

## ⚡ Prerequisites Check

Before starting, verify you have:
- ✅ Java 23.0.2 (check: `java -version`)
- ✅ Maven 3.6+ (check: `mvn -version`)
- ✅ Eclipse 2024-12 (or any Java IDE)

## 🚀 Steps to Run

### Option 1: Using Eclipse (Recommended)

1. **Extract the Project**
   ```
   Unzip SmartCollections.zip to your workspace
   ```

2. **Import into Eclipse**
   - Open Eclipse
   - File → Import → Maven → Existing Maven Projects
   - Browse to `SmartCollections` folder
   - Click Finish
   - Wait for Maven to download dependencies (~2 minutes)

3. **Run the Application**
   - Navigate to `src/main/java/com/smartcollections/MainApp.java`
   - Right-click → Run As → Java Application
   - The app window should appear!

### Option 2: Command Line

```bash
# Navigate to project directory
cd SmartCollections

# Build the project
mvn clean install

# Run the application
mvn javafx:run
```

## 📁 Test the Import Feature

1. **Create Test Folder**
   ```
   Create a folder anywhere on your computer
   Add some .txt, .pdf, or .mp3 files
   Create subfolders with more files
   ```

2. **Import in the App**
   - Click "Import..." button
   - Select your test folder
   - Watch the recursive scan work!
   - All files will be imported and indexed

## 🎯 Quick Feature Test

### Test Search & Ranking
1. Import some files with tags
2. Type keywords in search box
3. See results ranked by relevance

### Test Undo
1. Create a new item
2. Delete it
3. Click "Undo" - it comes back!

### Test Recently Viewed
1. Click on several items
2. Check the "Recently Viewed" panel
3. Most recent appears first

### Test Task Queue
1. Create a task for an item
2. Set priority and deadline
3. See it ordered automatically

## 🐛 Troubleshooting

### "No module found" error
```bash
# Make sure JavaFX is in your classpath
# Maven should handle this automatically
mvn clean install
```

### "FXML not found" error
```
Check that src/main/resources/fxml/MainView.fxml exists
```

### Build fails
```bash
# Clean and rebuild
mvn clean
mvn install
```

### Eclipse doesn't recognize JavaFX
```
Right-click project → Maven → Update Project
Check "Force Update of Snapshots/Releases"
```

## 📂 Where is My Data?

Your library data is saved in:
- **Linux/Mac**: `~/.smartcollections/library.dat`
- **Windows**: `C:\Users\<username>\.smartcollections\library.dat`

Backups are in: `.smartcollections/backups/`

## 🎓 Learning Checkpoints

As you explore the code, look for:

1. **Data Structures**
   - `LibraryService.java`: See ArrayList, HashMap, Stack, PriorityQueue
   - Comments explain why each was chosen

2. **Recursion**
   - `FileImportService.java`: `recursiveScan()` method
   - Classic recursive directory traversal

3. **Binary I/O**
   - `LibraryRepository.java`: ObjectOutputStream/ObjectInputStream
   - File format with header and versioning

4. **Design Patterns**
   - `Memento.java`: Memento pattern for undo
   - `MainController.java`: MVC architecture

5. **JavaFX**
   - `MainView.fxml`: Declarative UI
   - `MainController.java`: Event handlers with lambdas

## 📝 Next Steps

1. ✅ Get the app running
2. ✅ Test all features
3. ✅ Read the code comments
4. ✅ Run unit tests: `mvn test`
5. ✅ Customize and extend!

## 💡 Customization Ideas

- Add more file types to `FileImportService`
- Enhance search with fuzzy matching
- Add export to CSV functionality
- Create custom animations
- Add more sophisticated ranking

## 📞 Need Help?

Check:
- `README.md` for detailed documentation
- Inline code comments for explanations
- JavaDoc comments on classes and methods

---

**You're all set! Happy coding! 🎉**
