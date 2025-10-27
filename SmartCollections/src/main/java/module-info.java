module com.smartcollections {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.graphics;
    requires javafx.base;
    
    // Open packages for FXML access (reflection)
    opens com.smartcollections to javafx.fxml;
    opens com.smartcollections.ui to javafx.fxml;
    opens com.smartcollections.model to javafx.base;
    
    // Export packages
    exports com.smartcollections;
    exports com.smartcollections.ui;
    exports com.smartcollections.model;
    exports com.smartcollections.service;
    exports com.smartcollections.repository;
}
