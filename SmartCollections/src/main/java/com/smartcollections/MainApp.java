package com.smartcollections;

import com.smartcollections.service.LibraryService;
import com.smartcollections.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application class.
 * 
 * Entry point for the Smart Collections application.
 * 
 * ARCHITECTURE:
 * - Initializes JavaFX application
 * - Sets up main window and scene
 * - Initializes LibraryService (loads persisted data)
 * - Registers shutdown hook for auto-save
 * - Passes service to UI controllers
 */
public class MainApp extends Application {
    
    private LibraryService libraryService;
    
    /**
     * JavaFX start method - called after application initialization.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize service layer
            libraryService = new LibraryService();
            
            // Load FXML and create scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            
            // Get controller and inject service
            MainController controller = loader.getController();
            controller.setLibraryService(libraryService);
            
            // Configure main window
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setTitle("Smart Collections - Media & Notes Manager");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            
            // Show window
            primaryStage.show();
            
            System.out.println("Smart Collections started successfully");
            
        } catch (IOException e) {
            System.err.println("Failed to load application: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Called when application is closing.
     * Performs auto-save of library data.
     */
    @Override
    public void stop() {
        System.out.println("Application closing...");
        
        if (libraryService != null) {
            libraryService.autoSave();
        }
        
        System.out.println("Goodbye!");
    }
    
    /**
     * Main method - JVM entry point.
     * Launches the JavaFX application.
     */
    public static void main(String[] args) {
        // Register shutdown hook for graceful exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered");
        }));
        
        // Launch JavaFX application
        launch(args);
    }
}
