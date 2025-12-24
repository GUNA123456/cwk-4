/*
 * AdminDashboardController.java
 * 
 * Controller for Admin Dashboard - User Management
 * Allows admins to promote/demote users and delete users
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * AdminDashboardController - Handles admin user management operations
 * 
 * @author ntu-user
 */
public class AdminDashboardController {

    private User currentAdmin;
    private DBService service;

    // Sidebar menu buttons
    @FXML
    private Button btnUserManagementMenu;

    @FXML
    private Button btnFileManagementMenu;

    @FXML
    private Button btnAdvancedPanelMenu;

    @FXML
    private Button btnLogout;

    // Panels
    @FXML
    private javafx.scene.layout.AnchorPane userManagementPanel;

    @FXML
    private javafx.scene.layout.AnchorPane fileManagementPanel;

    @FXML
    private javafx.scene.layout.AnchorPane advancedPanel;

    // User Management Panel fields
    @FXML
    private Label lblAdminInfo;

    @FXML
    private TableView<User> tblUsers;

    @FXML
    private TableColumn<User, String> colUsername;

    @FXML
    private TableColumn<User, String> colRole;

    @FXML
    private Button btnPromoteToAdmin;

    @FXML
    private Button btnDemoteToUser;

    @FXML
    private Button btnDeleteUser;

    @FXML
    private Button btnRefresh;

    @FXML
    private Label lblUserStatus;

    /**
     * Initialize the admin dashboard with the current admin user
     * 
     * @param admin The logged-in admin user
     */
    public void initialise(User admin) {
        this.currentAdmin = admin;
        this.service = new DBService();

        if (admin != null) {
            lblAdminInfo.setText("Admin: " + admin.getUser());
        }

        // Setup table columns
        colUsername.setCellValueFactory(new PropertyValueFactory<>("user"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Show User Management panel by default
        showUserManagement(null);

        // Load users
        loadUsers();
    }

    /**
     * Show User Management panel
     */
    @FXML
    private void showUserManagement(ActionEvent event) {
        userManagementPanel.setVisible(true);
        fileManagementPanel.setVisible(false);
        advancedPanel.setVisible(false);

        // Update button styles
        btnUserManagementMenu.setStyle(
                "-fx-background-color: #5FA4B8; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnFileManagementMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnAdvancedPanelMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
    }

    /**
     * Show File Management panel
     */
    @FXML
    private void showFileManagement(ActionEvent event) {
        userManagementPanel.setVisible(false);
        fileManagementPanel.setVisible(true);
        advancedPanel.setVisible(false);

        // Update button styles
        btnUserManagementMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnFileManagementMenu.setStyle(
                "-fx-background-color: #5FA4B8; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnAdvancedPanelMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
    }

    /**
     * Show Advanced Panel
     */
    @FXML
    private void showAdvancedPanel(ActionEvent event) {
        userManagementPanel.setVisible(false);
        fileManagementPanel.setVisible(false);
        advancedPanel.setVisible(true);

        // Update button styles
        btnUserManagementMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnFileManagementMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
        btnAdvancedPanelMenu.setStyle(
                "-fx-background-color: #5FA4B8; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
    }

    /**
     * Load all users from database and display in table
     */
    private void loadUsers() {
        try {
            List<User> users = service.listUsers();
            ObservableList<User> data = FXCollections.observableArrayList(users);
            tblUsers.setItems(data);
            lblUserStatus.setText("Loaded " + users.size() + " users");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Failed to load users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle promote to admin button click
     */
    @FXML
    private void handlePromoteToAdmin(ActionEvent event) {
        User selectedUser = tblUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a user to promote.");
            return;
        }

        if ("admin".equalsIgnoreCase(selectedUser.getRole())) {
            showAlert(Alert.AlertType.INFORMATION, "Already Admin",
                    "User '" + selectedUser.getUser() + "' is already an admin.");
            return;
        }

        try {
            service.updateUserRole(selectedUser.getUser(), "admin");
            service.addLog(currentAdmin.getUser(), "PROMOTE_USER", null, null,
                    "Promoted user '" + selectedUser.getUser() + "' to admin");

            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "User '" + selectedUser.getUser() + "' promoted to admin.");
            loadUsers(); // Refresh table

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Operation Failed",
                    "Failed to promote user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle demote to user button click
     */
    @FXML
    private void handleDemoteToUser(ActionEvent event) {
        User selectedUser = tblUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a user to demote.");
            return;
        }

        if ("user".equalsIgnoreCase(selectedUser.getRole())) {
            showAlert(Alert.AlertType.INFORMATION, "Already User",
                    "User '" + selectedUser.getUser() + "' is already a regular user.");
            return;
        }

        // Prevent demoting yourself
        if (currentAdmin != null && selectedUser.getUser().equals(currentAdmin.getUser())) {
            showAlert(Alert.AlertType.ERROR, "Cannot Demote Self",
                    "You cannot demote yourself!");
            return;
        }

        try {
            service.updateUserRole(selectedUser.getUser(), "user");
            service.addLog(currentAdmin.getUser(), "DEMOTE_USER", null, null,
                    "Demoted user '" + selectedUser.getUser() + "' to regular user");

            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "User '" + selectedUser.getUser() + "' demoted to regular user.");
            loadUsers(); // Refresh table

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Operation Failed",
                    "Failed to demote user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle delete user button click
     */
    @FXML
    private void handleDeleteUser(ActionEvent event) {
        User selectedUser = tblUsers.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a user to delete.");
            return;
        }

        // Prevent deleting yourself
        if (currentAdmin != null && selectedUser.getUser().equals(currentAdmin.getUser())) {
            showAlert(Alert.AlertType.ERROR, "Cannot Delete Self",
                    "You cannot delete yourself!");
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete User");
        confirm.setContentText("Are you sure you want to delete user '" +
                selectedUser.getUser() + "'? This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    service.deleteUser(selectedUser.getUser());
                    service.addLog(currentAdmin.getUser(), "DELETE_USER", null, null,
                            "Deleted user '" + selectedUser.getUser() + "'");

                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "User '" + selectedUser.getUser() + "' deleted successfully.");
                    loadUsers(); // Refresh table

                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Operation Failed",
                            "Failed to delete user: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh(ActionEvent event) {
        loadUsers();
    }

    /**
     * Handle logout button click - return to login page
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Log logout
            if (currentAdmin != null) {
                service.addLog(currentAdmin.getUser(), "LOGOUT", null, null,
                        "Admin logged out");
            }

            Stage currentStage = (Stage) btnLogout.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 640, 480);
            currentStage.setScene(scene);
            currentStage.setTitle("Login");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show alert dialog
     * 
     * @param type    Alert type
     * @param title   Dialog title
     * @param message Dialog message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
