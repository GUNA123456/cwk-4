/*
 * RegisterController.java
 * 
 * Updated to use DBService for user registration
 * Step-3.1: Integrated with DBService.addUser()
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class for user registration
 * Updated to use DBService instead of direct DB calls
 *
 * @author ntu-user
 */
public class RegisterController {

    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;

    @FXML
    private Text fileText;

    @FXML
    private Button selectBtn;

    @FXML
    private void selectBtnHandler(ActionEvent event) throws IOException {
        Stage primaryStage = (Stage) selectBtn.getScene().getWindow();
        primaryStage.setTitle("Select a File");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            fileText.setText((String) selectedFile.getCanonicalPath());
        }
    }

    /**
     * Show alert dialog
     * 
     * @param type       Alert type
     * @param headerMsg  Header message
     * @param contentMsg Content message
     */
    private void showAlert(Alert.AlertType type, String headerMsg, String contentMsg) {
        Alert alert = new Alert(type);
        alert.setTitle(type == Alert.AlertType.ERROR ? "Error" : "Success");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        alert.showAndWait();
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();

        try {
            // Validate input
            String username = userTextField.getText();
            String password = passPasswordField.getText();
            String rePassword = rePassPasswordField.getText();

            if (username == null || username.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Username cannot be empty!");
                return;
            }

            if (password == null || password.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Password cannot be empty!");
                return;
            }

            // Check if passwords match
            if (!password.equals(rePassword)) {
                showAlert(Alert.AlertType.ERROR, "Password Mismatch",
                        "Passwords do not match. Please try again.");
                // Reload registration form
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("register.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 640, 480);
                secondaryStage.setScene(scene);
                secondaryStage.setTitle("Register a new User");
                secondaryStage.show();
                primaryStage.close();
                return;
            }

            // Use DBService to add user
            DBService service = new DBService();
            DB db = new DB(); // Only for password hashing

            // Hash the password
            String hashedPassword = db.generateSecurePassword(password);

            // Add user with default role "user"
            service.addUser(username, hashedPassword, "user");

            // Log the registration
            service.addLog(username, "REGISTER", null, null, "User registered successfully");

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Registration Successful",
                    "User '" + username + "' has been registered successfully!");

            // Navigate to secondary screen (dashboard)
            FXMLLoader loader = new FXMLLoader();
            String[] credentials = { username, password };
            loader.setLocation(getClass().getResource("secondary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            SecondaryController controller = loader.getController();
            secondaryStage.setTitle("Show users");
            controller.initialise(credentials);
            String msg = "User registered: " + username;
            secondaryStage.setUserData(msg);
            secondaryStage.show();
            primaryStage.close();

        } catch (SQLException e) {
            // Database error (e.g., duplicate username)
            showAlert(Alert.AlertType.ERROR, "Registration Failed",
                    "Could not register user. Username may already exist.\n\nError: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // General error
            showAlert(Alert.AlertType.ERROR, "Unexpected Error",
                    "An unexpected error occurred during registration.\n\nError: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) backLoginBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
