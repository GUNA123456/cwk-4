/*
 * SecondaryController.java
 * 
 * Updated to use User object and implement role-based access control
 * Step-3.3 & 3.4: User object integration and role-based navigation
 */
package com.mycompany.javafxapplication1;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * FXML Controller class for user dashboard
 * Updated to use User object and DBService
 * Implements role-based access control
 *
 * @author ntu-user
 */
public class SecondaryController {

    // Current logged-in user
    private User currentUser;

    @FXML
    private TextField userTextField;

    @FXML
    private TableView dataTableView;

    @FXML
    private Button secondaryButton;

    @FXML
    private Button refreshBtn;

    @FXML
    private TextField customTextField;

    // User Account button (visible for all users)
    @FXML
    private Button btnMyAccount;

    // Admin-only buttons
    @FXML
    private Button btnUserManagement;

    @FXML
    private Button btnSystemManagement;

    /**
     * Refresh button handler - displays user data from stage
     */
    @FXML
    private void RefreshBtnHandler(ActionEvent event) {
        Stage primaryStage = (Stage) customTextField.getScene().getWindow();
        customTextField.setText((String) primaryStage.getUserData());
    }

    /**
     * Logout and return to login screen
     */
    @FXML
    private void switchToPrimary() {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {
            // Log the logout action
            if (currentUser != null) {
                DBService service = new DBService();
                service.addLog(currentUser.getUser(), "LOGOUT", null, null,
                        "User logged out");
            }

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

    /**
     * Initialize controller with User object (NEW METHOD)
     * Step-3.3: Accept User object instead of String array
     * 
     * @param user Logged-in user object
     */
    public void initialise(User user) {
        this.currentUser = user;

        if (user == null) {
            System.err.println("Warning: User object is null in SecondaryController.initialise()");
            return;
        }

        // Update UI with user information
        userTextField.setText("Welcome, " + user.getUser() + " (" + user.getRole() + ")");

        // Load user table data using DBService
        try {
            DBService service = new DBService();
            List<User> users = service.listUsers();

            // Convert to ObservableList
            ObservableList<User> data = FXCollections.observableArrayList(users);

            // Create table columns
            TableColumn userCol = new TableColumn("User");
            userCol.setCellValueFactory(new PropertyValueFactory<>("user"));

            TableColumn roleCol = new TableColumn("Role");
            roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

            // Set data and columns
            dataTableView.setItems(data);
            dataTableView.getColumns().clear(); // Clear existing columns
            dataTableView.getColumns().addAll(userCol, roleCol);

            // Apply role-based access control
            applyRoleBasedAccess();

        } catch (SQLException ex) {
            Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE,
                    "Failed to load users", ex);
        }
    }

    /**
     * Legacy initialise method for backward compatibility
     * Converts String[] credentials to User object lookup
     * 
     * @param credentials [username, password]
     * @deprecated Use initialise(User) instead
     */
    public void initialise(String[] credentials) {
        if (credentials == null || credentials.length < 1) {
            System.err.println("Warning: Invalid credentials in SecondaryController.initialise()");
            return;
        }

        try {
            // Look up user by username
            DBService service = new DBService();
            User user = service.getUser(credentials[0]);

            if (user != null) {
                initialise(user);
            } else {
                // Fallback: create temporary user object
                User tempUser = new User(credentials[0], "", "user");
                initialise(tempUser);
            }
        } catch (SQLException e) {
            System.err.println("Error looking up user: " + e.getMessage());
            // Fallback: create temporary user object
            User tempUser = new User(credentials[0], "", "user");
            initialise(tempUser);
        }
    }

    /**
     * Apply role-based access control to UI elements
     * Step-4: Show/hide admin features based on role
     */
    private void applyRoleBasedAccess() {
        if (currentUser == null) {
            return;
        }

        String role = currentUser.getRole();
        boolean isAdmin = "admin".equalsIgnoreCase(role);

        // Show/hide admin buttons based on role
        if (btnUserManagement != null) {
            btnUserManagement.setVisible(isAdmin);
        }
        if (btnSystemManagement != null) {
            btnSystemManagement.setVisible(isAdmin);
        }

        if (isAdmin) {
            System.out.println("[SecondaryController] Admin user detected: " + currentUser.getUser());
            System.out.println("[SecondaryController] Admin buttons are now visible");
        } else {
            System.out.println("[SecondaryController] Regular user detected: " + currentUser.getUser());
            System.out.println("[SecondaryController] Admin buttons are hidden");
        }
    }

    /**
     * Open User Management Panel (admin only)
     */
    @FXML
    private void openUserManagementPanel(ActionEvent event) {
        try {
            // Log navigation
            DBService service = new DBService();
            service.addLog(currentUser.getUser(), "NAVIGATE", null, null,
                    "Opened User Management panel");

            Stage currentStage = (Stage) btnUserManagement.getScene().getWindow();
            Stage newStage = new Stage();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("admin_dashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.initialise(currentUser);

            Scene scene = new Scene(root, 640, 480);
            newStage.setScene(scene);
            newStage.setTitle("Admin Dashboard - User Management");
            newStage.show();
            currentStage.close();

        } catch (Exception e) {
            System.err.println("Error opening User Management panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Open System Management Panel (admin only)
     * Placeholder for future implementation
     */
    @FXML
    private void openSystemManagementPanel(ActionEvent event) {
        try {
            DBService service = new DBService();
            service.addLog(currentUser.getUser(), "NAVIGATE", null, null,
                    "Attempted to open System Management panel (not implemented)");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Coming Soon");
            alert.setHeaderText("System Management");
            alert.setContentText("System Management panel is not yet implemented.");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Open User Account Panel (available for all users)
     */
    @FXML
    private void openUserAccountPanel(ActionEvent event) {
        try {
            // Log navigation
            DBService service = new DBService();
            service.addLog(currentUser.getUser(), "NAVIGATE", null, null,
                    "Opened User Account panel");

            Stage currentStage = (Stage) btnMyAccount.getScene().getWindow();
            Stage newStage = new Stage();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("user_account.fxml"));
            Parent root = loader.load();

            UserAccountController controller = loader.getController();
            controller.initialise(currentUser);

            Scene scene = new Scene(root, 640, 480);
            newStage.setScene(scene);
            newStage.setTitle("My Account - " + currentUser.getUser());
            newStage.show();
            currentStage.close();

        } catch (Exception e) {
            System.err.println("Error opening User Account panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get current logged-in user
     * 
     * @return Current user object
     */
    public User getCurrentUser() {
        return currentUser;
    }
}
