package com.mycompany.javafxapplication1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Stage secondaryStage = new Stage();
        // Initialize database instance
        DB myObj = new DB();
        myObj.log("-------- Initializing Database with Safe Migrations ------------");

        // Run migrations instead of dropping tables
        // This ensures no data loss and safely adds new tables/columns
        MigrationService migrator = new MigrationService(myObj);
        try {
            migrator.migrate();
            myObj.log("---------- Database migrations completed successfully ----------");
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Database migration failed", ex);
            // Note: Application continues even if migration fails
            // In production, you might want to show an error dialog and exit
        }

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Primary View");
            secondaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}