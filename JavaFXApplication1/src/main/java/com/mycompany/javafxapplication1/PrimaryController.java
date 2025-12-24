/*
 * PrimaryController.java
 * 
 * Updated to use DBService for user authentication
 * Step-3.2: Integrated with DBService.validateUser() and getUser()
 */
package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

/**
 * FXML Controller class for user login
 * Updated to use DBService instead of direct DB calls
 *
 * @author ntu-user
 */
public class PrimaryController {

    @FXML
    private Button registerBtn;

    @FXML
    private TextField userTextField;

    @FXML
    private PasswordField passPasswordField;

    /**
     * Navigate to registration page
     */
    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Register a new User");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
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

    /**
     * Handle login button click
     * Updated to use DBService for authentication
     */
    @FXML
    private void switchToSecondary() {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();

        try {
            String username = userTextField.getText();
            String password = passPasswordField.getText();

            // Validate input
            if (username == null || username.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Username cannot be empty!");
                return;
            }

            if (password == null || password.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Password cannot be empty!");
                return;
            }

            // Use DBService to validate user
            DBService service = new DBService();
            boolean isValid = service.validateUser(username, password);

            if (isValid) {
                // Retrieve full User object
                User currentUser = service.getUser(username);

                if (currentUser == null) {
                    showAlert(Alert.AlertType.ERROR, "Login Error",
                            "User validation succeeded but user not found. Please contact support.");
                    return;
                }

                // Log successful login
                service.addLog(username, "LOGIN", null, null,
                        "User logged in successfully (role: " + currentUser.getRole() + ")");

                // Route based on user role
                FXMLLoader loader = new FXMLLoader();

                if ("admin".equals(currentUser.getRole())) {
                    // Admin user -> Admin Dashboard
                    loader.setLocation(getClass().getResource("admin_dashboard.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 900, 700);
                    secondaryStage.setScene(scene);

                    AdminDashboardController controller = loader.getController();
                    controller.initialise(currentUser);

                    secondaryStage.setTitle("Admin Dashboard - " + currentUser.getUser());

                } else {
                    // Regular user -> My Account
                    loader.setLocation(getClass().getResource("user_account.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 800, 600);
                    secondaryStage.setScene(scene);

                    UserAccountController controller = loader.getController();
                    controller.initialise(currentUser);

                    secondaryStage.setTitle("My Account - " + currentUser.getUser());
                }

                String msg = "Logged in as: " + currentUser.getUser() + " (" + currentUser.getRole() + ")";
                secondaryStage.setUserData(msg);
                secondaryStage.show();
                primaryStage.close();

            } else {
                // Invalid credentials
                showAlert(Alert.AlertType.ERROR, "Invalid Credentials",
                        "Invalid username or password. Please try again!");

                // Log failed login attempt
                service.addLog(username, "LOGIN_FAILED", null, null,
                        "Failed login attempt for user: " + username);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "A database error occurred during login.\n\nError: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Unexpected Error",
                    "An unexpected error occurred during login.\n\nError: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
