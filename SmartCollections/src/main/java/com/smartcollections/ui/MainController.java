package com.smartcollections.ui;

import com.smartcollections.model.Item;
import com.smartcollections.model.SearchResult;
import com.smartcollections.model.Task;
import com.smartcollections.service.FileImportService;
import com.smartcollections.service.LibraryService;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Main controller for the Smart Collections UI.
 * 
 * RESPONSIBILITIES:
 * - Handle user interactions (button clicks, text input, etc.)
 * - Update UI based on data changes
 * - Coordinate between UI components and service layer
 * - Manage animations and transitions
 * 
 * EVENT HANDLING:
 * - Uses FXML annotations for event binding
 * - Lambda expressions for simple handlers
 * - Inner classes for complex event logic
 */
public class MainController {
    
    // Service layer
    private LibraryService libraryService;
    private FileImportService importService;
    
    // Media player for audio/video preview
    private MediaPlayer mediaPlayer;
    private String currentMediaPath; // Track current media to avoid recreating player
    
    // ==================== FXML COMPONENTS ====================
    
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button clearSearchButton;
    
    @FXML private ListView<Item> itemListView;
    @FXML private ComboBox<String> categoryFilter;
    
    @FXML private VBox detailPane;
    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField tagsField;
    @FXML private Slider ratingSlider;
    @FXML private Label ratingLabel;
    @FXML private TextArea descriptionArea;
    @FXML private TextField filePathField;
    @FXML private Button browseButton;
    
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Button newButton;
    @FXML private Button undoButton;
    
    @FXML private ListView<Item> recentListView;
    @FXML private ListView<Task> taskListView;
    @FXML private Button addTaskButton;
    @FXML private Button processTaskButton;
    
    @FXML private MediaView mediaView;
    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button stopButton;
    @FXML private Button fullscreenButton;
    @FXML private Slider mediaTimeSlider;
    @FXML private Slider volumeSlider;
    @FXML private Label currentTimeLabel;
    @FXML private Label durationLabel;
    
    @FXML private Button importButton;
    @FXML private Label statusLabel;
    
    // Observable lists for UI binding
    private ObservableList<Item> itemList;
    private ObservableList<Item> recentList;
    private ObservableList<Task> taskList;
    
    // Currently selected item
    private Item currentItem;
    
    /**
     * FXML initialize method - called after FXML is loaded.
     * Sets up UI components and event handlers.
     */
    @FXML
    public void initialize() {
        // Initialize observable lists
        itemList = FXCollections.observableArrayList();
        recentList = FXCollections.observableArrayList();
        taskList = FXCollections.observableArrayList();
        
        // Bind lists to UI components
        itemListView.setItems(itemList);
        recentListView.setItems(recentList);
        taskListView.setItems(taskList);
        
        // Set up cell factories for custom display
        itemListView.setCellFactory(lv -> new ItemCell());
        recentListView.setCellFactory(lv -> new ItemCell());
        taskListView.setCellFactory(lv -> new TaskCell());
        
        // Set up event handlers using lambdas
        itemListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> displayItemDetails(newVal)
        );
        
        // Allow clicking items in Recently Viewed list
        recentListView.getSelectionModel().selectedItemProperty().addListener(
        	    (obs, oldVal, newVal) -> {
        	        if (newVal != null) {
        	            // Defer to avoid JavaFX IndexOutOfBoundsException during list modification
        	            Platform.runLater(() -> {
        	                // Select the item in the main list
        	                itemListView.getSelectionModel().select(newVal);
        	                displayItemDetails(newVal);
        	            });
        	        }
        	    }
        	);
        
        searchField.setOnAction(e -> performSearch());
        
        ratingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            ratingLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });
        
        // Initialize import service
        importService = new FileImportService();
        
        // Initially hide detail pane
        detailPane.setVisible(false);
        
        System.out.println("MainController initialized");
    }
    
    /**
     * Sets the library service (called by MainApp).
     */
    public void setLibraryService(LibraryService service) {
        this.libraryService = service;
        
        // Load initial data
        refreshItemList();
        refreshRecentList();
        refreshTaskList();
        updateStatistics();
        
        // Populate category filter
        populateCategoryFilter();
    }
    
    // ==================== SEARCH FUNCTIONALITY ====================
    
    /**
     * Performs search using the service layer.
     * Demonstrates case-insensitive search with PriorityQueue ranking.
     */
    @FXML
    private void performSearch() {
        String query = searchField.getText().trim();
        
        if (query.isEmpty()) {
            refreshItemList();
            return;
        }
        
        // Perform search (returns ranked results)
        List<SearchResult> results = libraryService.search(query);
        
        // Extract items from results
        itemList.clear();
        for (SearchResult result : results) {
            itemList.add(result.getItem());
        }
        
        // Animate results appearing (FadeTransition)
        fadeIn(itemListView);
        
        statusLabel.setText(String.format("Found %d results for '%s'", results.size(), query));
    }
    
    @FXML
    private void clearSearch() {
        searchField.clear();
        refreshItemList();
        statusLabel.setText("Search cleared");
    }
    
    // ==================== ITEM CRUD ====================
    
    @FXML
    private void createNewItem() {
        currentItem = null;
        clearDetailFields();
        detailPane.setVisible(true);
        fadeIn(detailPane);
        statusLabel.setText("Creating new item");
    }
    
    @FXML
    private void saveItem() {
        try {
            boolean isNewItem = false;
            
            if (currentItem == null) {
                // Create brand new item
                currentItem = new Item(titleField.getText(), categoryComboBox.getValue());
                isNewItem = true;
            }
            
            // Update item fields (modifying the copy, not the original)
            currentItem.setTitle(titleField.getText());
            currentItem.setCategory(categoryComboBox.getValue());
            currentItem.setRating((int) ratingSlider.getValue());
            currentItem.setDescription(descriptionArea.getText());
            currentItem.setFilePath(filePathField.getText());
            
            // Parse tags (comma-separated)
            currentItem.clearTags();
            String[] tags = tagsField.getText().split(",");
            for (String tag : tags) {
                currentItem.addTag(tag.trim());
            }
            
            // Save to service
            // Check if item exists in service (not in UI list) to determine update vs create
            if (!isNewItem && libraryService.getAllItems().stream()
                    .anyMatch(item -> item.getId().equals(currentItem.getId()))) {
                // Updating existing item - service will create memento of OLD state
                libraryService.updateItem(currentItem);
            } else {
                // Creating new item
                libraryService.addItem(currentItem);
            }
            
            refreshItemList();
            refreshRecentList();  // Update in case the saved item was recently viewed
            statusLabel.setText("Item saved: " + currentItem.getTitle());
            
        } catch (Exception e) {
            showError("Failed to save item", e.getMessage());
        }
    }
    
    @FXML
    private void deleteItem() {
        if (currentItem == null) return;
        
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Item");
        alert.setContentText("Are you sure you want to delete: " + currentItem.getTitle() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            libraryService.deleteItem(currentItem.getId());
            refreshItemList();
            clearDetailFields();
            detailPane.setVisible(false);
            statusLabel.setText("Item deleted");
        }
    }
    
    @FXML
    private void undoLastAction() {
        if (libraryService.canUndo()) {
            // Get the current item ID before undo (if any)
            String currentItemId = (currentItem != null) ? currentItem.getId() : null;
            
            // Perform undo
            libraryService.undo();
            
            // Refresh all lists
            refreshItemList();
            refreshRecentList();
            refreshTaskList();
            
            // If we had an item displayed, refresh its details from the service
            // This ensures we show the restored state after undo
            if (currentItemId != null) {
                Item restoredItem = libraryService.getAllItems().stream()
                    .filter(item -> item.getId().equals(currentItemId))
                    .findFirst()
                    .orElse(null);
                
                if (restoredItem != null) {
                    // Redisplay the restored item
                    displayItemDetails(restoredItem);
                } else {
                    // Item was deleted by undo, clear detail pane
                    detailPane.setVisible(false);
                    currentItem = null;
                }
            }
            
            statusLabel.setText("Action undone successfully");
            
            // Show confirmation with fade animation
            fadeIn(itemListView);
            
        } else {
            statusLabel.setText("Nothing to undo");
        }
    }
    
    // ==================== FILE IMPORT ====================
    
    /**
     * Opens file chooser to browse for a media file.
     */
    @FXML
    private void browseFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Media File");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"),
            new javafx.stage.FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav"),
            new javafx.stage.FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi"),
            new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.pdf", "*.txt", "*.md")
        );
        
        File selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            statusLabel.setText("File selected: " + selectedFile.getName());
        }
    }
    
    // ==================== FILE IMPORT ====================
    
    @FXML
    private void importDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Directory to Import");
        File directory = chooser.showDialog(importButton.getScene().getWindow());
        
        if (directory != null) {
            // Show progress
            statusLabel.setText("Importing from: " + directory.getName() + "...");
            
            // Perform import (this uses recursive algorithm)
            FileImportService.ImportResult result = importService.importDirectory(
                directory.getAbsolutePath(),
                libraryService.getAllItems().stream()
                    .map(Item::getFilePath)
                    .filter(path -> path != null)
                    .collect(java.util.stream.Collectors.toSet())
            );
            
            // Add imported items to library
            for (Item item : result.getImportedItems()) {
                libraryService.addItem(item);
            }
            
            // Refresh UI
            refreshItemList();
            
            // Show results
            showInfo("Import Complete", result.getSummary());
            statusLabel.setText(String.format("Imported %d items", result.getFilesImported()));
        }
    }
    
    // ==================== MEDIA PLAYBACK ====================
    
    /**
     * Processes the next highest-priority task.
     */
    @FXML
    private void processNextTask() {
        Task task = libraryService.pollNextTask();
        if (task != null) {
            statusLabel.setText("Processing task: " + task.getDescription());
            refreshTaskList();
            
            // Show info dialog
            showInfo("Task Processing", 
                String.format("Processing task: %s\n\nPriority: %s\nDeadline: %s", 
                    task.getDescription(),
                    task.getPriority(),
                    task.getDeadline()));
        } else {
            statusLabel.setText("No tasks to process");
        }
    }
    
    /**
     * Opens a dialog to create a new task.
     */
    @FXML
    private void createNewTask() {
        // Create dialog
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Create New Task");
        dialog.setHeaderText("Add a new study task with priority and deadline");
        
        // Set button types
        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Create the form
        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(20));
        
        // Item selection (optional - link task to an item)
        Label itemLabel = new Label("Link to Item (optional):");
        ComboBox<Item> itemCombo = new ComboBox<>();
        itemCombo.setPromptText("Select an item...");
        itemCombo.getItems().add(null); // Add "None" option
        itemCombo.getItems().addAll(libraryService.getAllItems());
        itemCombo.setMaxWidth(Double.MAX_VALUE);
        
        // Custom cell factory for item combo
        itemCombo.setCellFactory(lv -> new ListCell<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("(No item)");
                } else {
                    setText(item.getTitle());
                }
            }
        });
        itemCombo.setButtonCell(new ListCell<Item>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("(No item)");
                } else {
                    setText(item.getTitle());
                }
            }
        });
        
        // Task description
        Label descLabel = new Label("Task Description:*");
        TextField descField = new TextField();
        descField.setPromptText("e.g., Review Chapter 5, Complete Assignment");
        
        // Priority selection
        Label priorityLabel = new Label("Priority:*");
        ComboBox<Task.TaskPriority> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll(Task.TaskPriority.values());
        priorityCombo.setValue(Task.TaskPriority.MEDIUM);
        priorityCombo.setMaxWidth(Double.MAX_VALUE);
        
        // Deadline date picker
        Label deadlineLabel = new Label("Deadline:*");
        DatePicker deadlinePicker = new DatePicker();
        deadlinePicker.setPromptText("Select deadline date");
        deadlinePicker.setValue(java.time.LocalDate.now().plusDays(1));
        
        // Deadline time
        Label timeLabel = new Label("Time:*");
        HBox timeBox = new HBox(5);
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 23);
        hourSpinner.setPrefWidth(70);
        Label colonLabel = new Label(":");
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 59);
        minuteSpinner.setPrefWidth(70);
        timeBox.getChildren().addAll(hourSpinner, colonLabel, minuteSpinner);
        
        // Notes (optional)
        Label notesLabel = new Label("Notes (optional):");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Additional notes...");
        notesArea.setPrefRowCount(3);
        
        // Add all to form
        form.getChildren().addAll(
            itemLabel, itemCombo,
            descLabel, descField,
            priorityLabel, priorityCombo,
            deadlineLabel, deadlinePicker,
            timeLabel, timeBox,
            notesLabel, notesArea
        );
        
        dialog.getDialogPane().setContent(form);
        
        // Enable/disable create button based on input
        javafx.scene.Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);
        
        // Validation
        descField.textProperty().addListener((obs, old, newVal) -> {
            createButton.setDisable(newVal.trim().isEmpty());
        });
        
        // Convert result to Task when create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Item selectedItem = itemCombo.getValue();
                String itemId = selectedItem != null ? selectedItem.getId() : null;
                String itemTitle = selectedItem != null ? selectedItem.getTitle() : "General Task";
                
                String description = descField.getText().trim();
                Task.TaskPriority priority = priorityCombo.getValue();
                
                // Combine date and time
                java.time.LocalDate date = deadlinePicker.getValue();
                int hour = hourSpinner.getValue();
                int minute = minuteSpinner.getValue();
                java.time.LocalDateTime deadline = java.time.LocalDateTime.of(
                    date.getYear(), date.getMonth(), date.getDayOfMonth(),
                    hour, minute
                );
                
                Task task = new Task(itemId, itemTitle, description, priority, deadline);
                
                // Add notes if provided
                String notes = notesArea.getText().trim();
                if (!notes.isEmpty()) {
                    task.setNotes(notes);
                }
                
                return task;
            }
            return null;
        });
        
        // Show dialog and process result
        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(task -> {
            libraryService.addTask(task);
            refreshTaskList();
            statusLabel.setText("Task created: " + task.getDescription());
            
            // Show success feedback with fade
            fadeIn(taskListView);
        });
    }
    
    // ==================== MEDIA PLAYBACK ====================
    
    @FXML
    private void playMedia() {
        if (currentItem != null && currentItem.hasMedia()) {
            try {
                String mediaPath = currentItem.getFilePath();
                if (mediaPath == null || mediaPath.isEmpty()) {
                    // Try media URL if file path is empty
                    mediaPath = currentItem.getMediaUrl();
                }
                
                if (mediaPath != null && !mediaPath.isEmpty()) {
                    // Check if we need to load new media or just resume
                    if (mediaPlayer != null && mediaPath.equals(currentMediaPath)) {
                        // Resume from current position
                        MediaPlayer.Status status = mediaPlayer.getStatus();
                        if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.STOPPED) {
                            mediaPlayer.play();
                            statusLabel.setText("▶ Playing: " + currentItem.getTitle());
                        }
                    } else {
                        // Load and play new media
                        loadMediaForCurrentItem();
                    }
                }
            } catch (Exception e) {
                showError("Media Error", "Failed to play media: " + e.getMessage());
            }
        } else {
            statusLabel.setText("No media available for this item");
        }
    }
    
    @FXML
    private void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            statusLabel.setText("⏸ Paused: " + (currentItem != null ? currentItem.getTitle() : ""));
        }
    }
    
    @FXML
    private void stopMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            statusLabel.setText("⏹ Stopped");
        }
    }
    
    /**
     * Loads media for the current item into the MediaView.
     * Called when an item with media is selected.
     */
    private void loadMediaForCurrentItem() {
        // Clear any existing media player first
        disposeMediaPlayer();
        
        if (currentItem == null || !currentItem.hasMedia()) {
            // Disable media controls if no media
            mediaTimeSlider.setDisable(true);
            volumeSlider.setDisable(true);
            currentTimeLabel.setText("00:00");
            durationLabel.setText("00:00");
            return;
        }
        
        try {
            String mediaPath = currentItem.getFilePath();
            if (mediaPath == null || mediaPath.isEmpty()) {
                mediaPath = currentItem.getMediaUrl();
            }
            
            if (mediaPath != null && !mediaPath.isEmpty()) {
                // Create Media object (supports both local files and URLs)
                Media media;
                if (mediaPath.startsWith("http://") || mediaPath.startsWith("https://")) {
                    media = new Media(mediaPath); // URL
                } else {
                    File mediaFile = new File(mediaPath);
                    if (!mediaFile.exists()) {
                        statusLabel.setText("⚠ Media file not found: " + mediaFile.getName());
                        mediaTimeSlider.setDisable(true);
                        volumeSlider.setDisable(true);
                        return;
                    }
                    media = new Media(mediaFile.toURI().toString()); // Local file
                }
                
                // Create and configure MediaPlayer
                mediaPlayer = new MediaPlayer(media);
                currentMediaPath = mediaPath;
                mediaView.setMediaPlayer(mediaPlayer);
                
                // Set up media player ready listener
                mediaPlayer.setOnReady(() -> {
                    // Update duration label
                    Duration totalDuration = mediaPlayer.getTotalDuration();
                    durationLabel.setText(formatTime(totalDuration));
                    mediaTimeSlider.setMax(totalDuration.toSeconds());
                    mediaTimeSlider.setDisable(false);
                    volumeSlider.setDisable(false);
                    
                    statusLabel.setText("✓ Media loaded: " + currentItem.getTitle());
                });
                
                // Set up current time listener (updates time label and slider)
                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (!mediaTimeSlider.isValueChanging()) {
                        currentTimeLabel.setText(formatTime(newTime));
                        mediaTimeSlider.setValue(newTime.toSeconds());
                    }
                });
                
                // Set up seek slider listener
                mediaTimeSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
                    if (!isChanging) {
                        mediaPlayer.seek(Duration.seconds(mediaTimeSlider.getValue()));
                    }
                });
                
                // Set up volume slider listener
                volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(newValue.doubleValue());
                    }
                });
                
                // Set initial volume
                mediaPlayer.setVolume(volumeSlider.getValue());
                
                // Set up error handler
                mediaPlayer.setOnError(() -> {
                    String error = mediaPlayer.getError() != null ? 
                                  mediaPlayer.getError().getMessage() : "Unknown error";
                    statusLabel.setText("⚠ Media error: " + error);
                    mediaTimeSlider.setDisable(true);
                });
                
                // Set up end of media listener (auto-stop at end)
                mediaPlayer.setOnEndOfMedia(() -> {
                    statusLabel.setText("✓ Playback complete");
                    mediaPlayer.stop();
                });
                
                // Auto-play the media
                mediaPlayer.play();
                statusLabel.setText("▶ Playing: " + currentItem.getTitle());
            }
        } catch (Exception e) {
            statusLabel.setText("⚠ Failed to load media: " + e.getMessage());
            mediaTimeSlider.setDisable(true);
            volumeSlider.setDisable(true);
            e.printStackTrace();
        }
    }
    
    /**
     * Formats a Duration into a readable time string (MM:SS or HH:MM:SS).
     */
    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) {
            return "00:00";
        }
        
        int totalSeconds = (int) duration.toSeconds();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Properly disposes of the current media player.
     * Called when switching items or clearing the view.
     */
    private void disposeMediaPlayer() {
        // Exit fullscreen if active
        if (isMediaFullscreen) {
            exitMediaFullscreen();
        }
        
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            currentMediaPath = null;
            mediaView.setMediaPlayer(null);
        }
        
        // Reset media controls
        mediaTimeSlider.setValue(0);
        mediaTimeSlider.setDisable(true);
        volumeSlider.setDisable(true);
        currentTimeLabel.setText("00:00");
        durationLabel.setText("00:00");
    }
    
    // Fullscreen stage for media-only view
    private Stage fullscreenStage;
    private VBox originalParent;
    private boolean isMediaFullscreen = false;
    
    /**
     * Toggles fullscreen mode for ONLY the MediaView (not the whole app).
     * Creates a separate fullscreen window showing just the media player.
     */
    @FXML
    private void toggleFullscreen() {
        if (mediaPlayer == null) {
            statusLabel.setText("⚠ No media loaded");
            return;
        }
        
        if (!isMediaFullscreen) {
            // Enter fullscreen - show only MediaView
            enterMediaFullscreen();
        } else {
            // Exit fullscreen - return MediaView to original layout
            exitMediaFullscreen();
        }
    }
    
    /**
     * Enters fullscreen mode by creating a new window with only the MediaView.
     */
    private void enterMediaFullscreen() {
        try {
            // Save original parent
            originalParent = (VBox) mediaView.getParent();
            
            // Remove MediaView from current layout
            originalParent.getChildren().remove(mediaView);
            
            // Create fullscreen stage
            fullscreenStage = new Stage();
            fullscreenStage.setTitle("Media Player - Fullscreen");
            
            // Create black background pane for the media
            StackPane fullscreenRoot = new StackPane();
            fullscreenRoot.setStyle("-fx-background-color: black;");
            
            // Add MediaView to fullscreen pane
            mediaView.setFitWidth(Screen.getPrimary().getBounds().getWidth());
            mediaView.setFitHeight(Screen.getPrimary().getBounds().getHeight());
            fullscreenRoot.getChildren().add(mediaView);
            
            // Create scene and show fullscreen
            Scene fullscreenScene = new Scene(fullscreenRoot);
            fullscreenStage.setScene(fullscreenScene);
            fullscreenStage.setFullScreen(true);
            fullscreenStage.setFullScreenExitHint("Press ESC to exit fullscreen");
            
            // Handle ESC key or window close
            fullscreenStage.setOnCloseRequest(e -> exitMediaFullscreen());
            fullscreenScene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    exitMediaFullscreen();
                }
            });
            
            fullscreenStage.show();
            isMediaFullscreen = true;
            statusLabel.setText("⛶ Media fullscreen (Press ESC to exit)");
            
        } catch (Exception e) {
            statusLabel.setText("⚠ Fullscreen error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Exits fullscreen mode by returning the MediaView to its original position.
     */
    private void exitMediaFullscreen() {
        if (fullscreenStage != null) {
            // Close fullscreen window
            fullscreenStage.close();
            fullscreenStage = null;
            
            // Return MediaView to original parent
            if (originalParent != null) {
                // Reset MediaView size
                mediaView.setFitWidth(400);
                mediaView.setFitHeight(300);
                
                // Find the correct position (first child in the VBox)
                if (!originalParent.getChildren().contains(mediaView)) {
                    originalParent.getChildren().add(0, mediaView);
                }
            }
            
            isMediaFullscreen = false;
            statusLabel.setText("Normal view");
        }
    }
    
    // ==================== UI UPDATE METHODS ====================
    
    /**
     * Displays item details in the detail pane.
     * 
     * IMPORTANT: Works with a COPY of the item to prevent premature modification
     * of the original item before undo memento is created.
     */
    private void displayItemDetails(Item item) {
        if (item == null) {
            detailPane.setVisible(false);
            return;
        }
        
        // Work with a COPY to prevent modifying the original before save
        // This ensures the undo memento captures the correct "before" state
        currentItem = new Item(item);  // Deep copy using copy constructor
        
        // Mark the ORIGINAL item as recently viewed and refresh recent list
        libraryService.getItem(item.getId()); // This triggers markAsViewed internally
        Platform.runLater(() -> refreshRecentList());
        
        // Populate fields with null safety
        titleField.setText(currentItem.getTitle() != null ? currentItem.getTitle() : "");
        categoryComboBox.setValue(currentItem.getCategory() != null ? currentItem.getCategory() : "Other");
        ratingSlider.setValue(currentItem.getRating());
        descriptionArea.setText(currentItem.getDescription() != null ? currentItem.getDescription() : "");
        filePathField.setText(currentItem.getFilePath() != null ? currentItem.getFilePath() : "");
        
        // Format tags
        tagsField.setText(String.join(", ", currentItem.getTags()));
        
        // Load media preview if item has media
        loadMediaForCurrentItem();
        
        // Show detail pane with animation
        detailPane.setVisible(true);
        fadeIn(detailPane);
    }
    
    private void clearDetailFields() {
        titleField.clear();
        categoryComboBox.setValue("Other");
        ratingSlider.setValue(0);
        descriptionArea.clear();
        filePathField.clear();
        tagsField.clear();
        
        // Dispose media player when creating new item
        disposeMediaPlayer();
    }
    
    private void refreshItemList() {
        itemList.clear();
        itemList.addAll(libraryService.getAllItems());
    }
    
    private void refreshRecentList() {
        recentList.clear();
        recentList.addAll(libraryService.getRecentlyViewed());
    }
    
    private void refreshTaskList() {
        taskList.clear();
        taskList.addAll(libraryService.getAllTasks());
    }
    
    private void populateCategoryFilter() {
        // Add predefined categories to detail pane dropdown
        categoryComboBox.getItems().addAll(
            "Text Document", "PDF Document", "Audio", "Video", "Other"
        );
        categoryComboBox.setValue("Other");
        
        // Add categories to filter dropdown (with "All" option)
        categoryFilter.getItems().add("All Categories");
        categoryFilter.getItems().addAll(
            "Text Document", "PDF Document", "Audio", "Video", "Other"
        );
        categoryFilter.setValue("All Categories");
        
        // Add listener to filter items when category changes
        categoryFilter.setOnAction(e -> filterByCategory());
    }
    
    /**
     * Filters items by selected category.
     */
    private void filterByCategory() {
        String selected = categoryFilter.getValue();
        if (selected == null || selected.equals("All Categories")) {
            refreshItemList();
        } else {
            itemList.clear();
            itemList.addAll(libraryService.getItemsByCategory(selected));
        }
        statusLabel.setText("Filtered by: " + selected);
    }
    
    private void updateStatistics() {
        // Update UI with library statistics
        // This could display in a status bar or separate panel
    }
    
    // ==================== ANIMATIONS ====================
    
    /**
     * Simple fade-in animation for UI elements.
     * Demonstrates use of JavaFX animations.
     */
    private void fadeIn(javafx.scene.Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }
    
    // ==================== UTILITY METHODS ====================
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==================== CUSTOM CELL FACTORIES ====================
    
    /**
     * Custom cell for displaying Items in ListView.
     */
    private class ItemCell extends ListCell<Item> {
        @Override
        protected void updateItem(Item item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(String.format("%s [%s] ★%d", 
                    item.getTitle(), 
                    item.getCategory(),
                    item.getRating()));
            }
        }
    }
    
    /**
     * Custom cell for displaying Tasks in ListView.
     */
    private class TaskCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            
            if (empty || task == null) {
                setText(null);
                setGraphic(null);
            } else {
                String status = task.isOverdue() ? "⚠ OVERDUE" : 
                               task.isDueSoon() ? "⏰ DUE SOON" : "";
                setText(String.format("[%s] %s %s", 
                    task.getPriority(), 
                    task.getDescription(),
                    status));
            }
        }
    }
}