/*
 * AdvancedPanelController.java
 * 
 * Controller for Advanced Tools Panel
 * Handles chunk checking, CRC32 validation, access control, and logs viewing
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdvancedPanelController - Controller for Advanced Tools Panel
 * 
 * @author ntu-user
 */
public class AdvancedPanelController {

    // Current user
    private User currentUser;
    private Path workspacePath;
    private DBService service;

    // Sidebar buttons
    @FXML
    private Button btnUserManagementMenu;

    @FXML
    private Button btnFileManagementMenu;

    @FXML
    private Button btnAdvancedPanelMenu;

    @FXML
    private Button btnLogout;

    // Section 1: Chunk Checker
    @FXML
    private TextField fileNameFieldChunks;

    @FXML
    private Button checkChunksButton;

    @FXML
    private TextArea chunksOutputArea;

    // Section 2: CRC32 Validator
    @FXML
    private TextField fileNameFieldCRC32;

    @FXML
    private Button checkCRCButton;

    @FXML
    private TextArea crcOutputArea;

    // Section 3: Access Control Checker
    @FXML
    private TextField fileNameFieldAccess;

    @FXML
    private Button checkAccessButton;

    @FXML
    private Label accessResultLabel;

    // Section 4: Logs Viewer
    @FXML
    private DatePicker logFromDate;

    @FXML
    private DatePicker logToDate;

    @FXML
    private TextField containerIdField;

    @FXML
    private Button loadLogsButton;

    @FXML
    private TableView<LogEntry> logsTable;

    @FXML
    private TableColumn<LogEntry, String> colTimestamp;

    @FXML
    private TableColumn<LogEntry, String> colUsername;

    @FXML
    private TableColumn<LogEntry, String> colAction;

    @FXML
    private TableColumn<LogEntry, String> colContainer;

    @FXML
    private TableColumn<LogEntry, String> colDetails;

    /**
     * Initialize the controller with user context
     * 
     * @param user Current logged-in user
     */
    public void initialise(User user) {
        this.currentUser = user;
        this.service = new DBService();
        this.workspacePath = FileService.getUserWorkspace(user.getUser());

        // Ensure workspace exists
        try {
            FileService.ensureWorkspaceExists(user.getUser());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Workspace Error",
                    "Failed to create workspace: " + e.getMessage());
        }

        // Setup table columns
        setupLogsTable();

        // Log access to advanced panel
        try {
            service.addLog(currentUser.getUser(), "ADVANCED_PANEL_OPEN", null, null,
                    "Opened Advanced Tools Panel");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup logs table columns
     */
    private void setupLogsTable() {
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colContainer.setCellValueFactory(new PropertyValueFactory<>("containerId"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
    }

    /**
     * Handle chunk existence check
     */
    @FXML
    private void handleCheckChunks(ActionEvent event) {
        String filename = fileNameFieldChunks.getText().trim();

        if (filename.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required",
                    "Please enter a filename");
            return;
        }

        try {
            // Load metadata file
            Path metaFile = workspacePath.resolve(filename + ".meta.json");

            if (!Files.exists(metaFile)) {
                chunksOutputArea.setText("ERROR: Metadata file not found: " + filename + ".meta.json\n\n" +
                        "Please ensure the metadata file exists in your workspace.");
                return;
            }

            JsonObject metadata = ChunkService.loadMetadata(metaFile);

            // Check chunks existence
            Map<String, Boolean> results = ChunkService.checkChunksExist(workspacePath, metadata);

            // Build output
            StringBuilder output = new StringBuilder();
            output.append("═══════════════════════════════════════════════════\n");
            output.append("  CHUNK EXISTENCE CHECK: ").append(filename).append("\n");
            output.append("═══════════════════════════════════════════════════\n\n");

            if (results.isEmpty()) {
                output.append("No chunks defined in metadata.\n");
            } else {
                int existCount = 0;
                int totalCount = results.size();

                for (Map.Entry<String, Boolean> entry : results.entrySet()) {
                    String chunkName = entry.getKey();
                    boolean exists = entry.getValue();

                    output.append(exists ? "✓ " : "✗ ");
                    output.append(chunkName);
                    output.append(": ");
                    output.append(exists ? "EXISTS" : "MISSING");
                    output.append("\n");

                    if (exists)
                        existCount++;
                }

                output.append("\n");
                output.append("───────────────────────────────────────────────────\n");
                output.append(String.format("Summary: %d/%d chunks exist\n", existCount, totalCount));

                if (existCount == totalCount) {
                    output.append("Status: ALL CHUNKS PRESENT ✓\n");
                } else {
                    output.append("Status: SOME CHUNKS MISSING ✗\n");
                }
            }

            chunksOutputArea.setText(output.toString());

            // Log action
            service.addLog(currentUser.getUser(), "CHUNK_CHECK", filename, null,
                    "Checked chunks for " + filename);

        } catch (IOException e) {
            chunksOutputArea.setText("ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            chunksOutputArea.setText("ERROR: Unexpected error - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle CRC32 validation
     */
    @FXML
    private void handleCheckCRC(ActionEvent event) {
        String filename = fileNameFieldCRC32.getText().trim();

        if (filename.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required",
                    "Please enter a filename");
            return;
        }

        try {
            // Load metadata file
            Path metaFile = workspacePath.resolve(filename + ".meta.json");

            if (!Files.exists(metaFile)) {
                crcOutputArea.setText("ERROR: Metadata file not found: " + filename + ".meta.json\n\n" +
                        "Please ensure the metadata file exists in your workspace.");
                return;
            }

            JsonObject metadata = ChunkService.loadMetadata(metaFile);

            if (!metadata.has("chunks")) {
                crcOutputArea.setText("ERROR: No chunks defined in metadata");
                return;
            }

            // Build output
            StringBuilder output = new StringBuilder();
            output.append("═══════════════════════════════════════════════════\n");
            output.append("  CRC32 INTEGRITY VALIDATION: ").append(filename).append("\n");
            output.append("═══════════════════════════════════════════════════\n\n");

            JsonArray chunks = metadata.get("chunks").getAsJsonArray();
            int validCount = 0;
            int totalCount = chunks.size();

            for (JsonElement element : chunks) {
                JsonObject chunk = element.getAsJsonObject();
                String chunkName = chunk.get("name").getAsString();
                String expectedCRC = chunk.get("crc32").getAsString();

                Path chunkPath = workspacePath.resolve(chunkName);

                if (!Files.exists(chunkPath)) {
                    output.append("✗ ").append(chunkName).append(": FILE NOT FOUND\n");
                    continue;
                }

                // Calculate actual CRC32
                long actualCRCLong = ChunkService.computeChunkCRC(chunkPath);
                String actualCRC = Long.toHexString(actualCRCLong).toUpperCase();

                // Pad with zeros if needed (CRC32 is 8 hex digits)
                while (actualCRC.length() < 8) {
                    actualCRC = "0" + actualCRC;
                }

                boolean match = expectedCRC.equalsIgnoreCase(actualCRC);

                output.append(match ? "✓ " : "✗ ");
                output.append(chunkName).append(": ");
                output.append(match ? "OK" : "CORRUPTED");
                output.append("\n");
                output.append("  Expected: ").append(expectedCRC).append("\n");
                output.append("  Actual:   ").append(actualCRC).append("\n\n");

                if (match)
                    validCount++;
            }

            output.append("───────────────────────────────────────────────────\n");
            output.append(String.format("Summary: %d/%d chunks valid\n", validCount, totalCount));

            if (validCount == totalCount) {
                output.append("Status: ALL CHUNKS VALID ✓\n");
            } else {
                output.append("Status: SOME CHUNKS CORRUPTED ✗\n");
            }

            crcOutputArea.setText(output.toString());

            // Log action
            service.addLog(currentUser.getUser(), "CRC_CHECK", filename, null,
                    String.format("CRC32 validation: %d/%d valid", validCount, totalCount));

        } catch (IOException e) {
            crcOutputArea.setText("ERROR: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            crcOutputArea.setText("ERROR: Unexpected error - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle access control check
     */
    @FXML
    private void handleCheckAccess(ActionEvent event) {
        String filename = fileNameFieldAccess.getText().trim();

        if (filename.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required",
                    "Please enter a filename");
            return;
        }

        try {
            // Load metadata file
            Path metaFile = workspacePath.resolve(filename + ".meta.json");

            if (!Files.exists(metaFile)) {
                accessResultLabel.setText("ERROR: Metadata file not found");
                accessResultLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                return;
            }

            JsonObject metadata = ChunkService.loadMetadata(metaFile);

            // Check access
            boolean hasAccess = AccessService.hasAccess(currentUser, metadata);

            // Determine reason
            String reason = "";
            if (hasAccess) {
                if ("admin".equals(currentUser.getRole())) {
                    reason = " (Admin Override)";
                } else if (AccessService.isOwner(currentUser, metadata)) {
                    reason = " (Owner)";
                } else if (AccessService.isInAllowedUsers(currentUser, metadata)) {
                    reason = " (Allowed User)";
                }
            } else {
                reason = " (Not owner or allowed user)";
            }

            // Display result
            accessResultLabel.setText("ACCESS: " + (hasAccess ? "ALLOWED" : "DENIED") + reason);
            accessResultLabel.setStyle(hasAccess
                    ? "-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 16px;"
                    : "-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-font-size: 16px;");

            // Log action
            service.addLog(currentUser.getUser(), "ACCESS_CHECK", filename, null,
                    "Access: " + (hasAccess ? "ALLOWED" : "DENIED") + reason);

        } catch (IOException e) {
            accessResultLabel.setText("ERROR: " + e.getMessage());
            accessResultLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            e.printStackTrace();
        } catch (Exception e) {
            accessResultLabel.setText("ERROR: Unexpected error");
            accessResultLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            e.printStackTrace();
        }
    }

    /**
     * Handle load logs
     */
    @FXML
    private void handleLoadLogs(ActionEvent event) {
        try {
            // Get filters
            LocalDate fromDate = logFromDate.getValue();
            LocalDate toDate = logToDate.getValue();
            String containerId = containerIdField.getText().trim();

            // Query all logs (DBService only has listAllLogs method)
            List<LogEntry> logs = service.listAllLogs();

            // Filter by username if not admin
            if (!"admin".equals(currentUser.getRole())) {
                String username = currentUser.getUser();
                logs = logs.stream()
                        .filter(log -> username.equals(log.getUsername()))
                        .collect(Collectors.toList());
            }

            // Filter by date if specified
            if (fromDate != null || toDate != null) {
                logs = logs.stream()
                        .filter(log -> {
                            try {
                                LocalDate logDate = LocalDate.parse(log.getTimestamp().substring(0, 10));
                                if (fromDate != null && logDate.isBefore(fromDate))
                                    return false;
                                if (toDate != null && logDate.isAfter(toDate))
                                    return false;
                                return true;
                            } catch (Exception e) {
                                return true; // Include if can't parse
                            }
                        })
                        .collect(Collectors.toList());
            }

            // Filter by container if specified
            if (!containerId.isEmpty()) {
                String finalContainerId = containerId;
                logs = logs.stream()
                        .filter(log -> finalContainerId.equals(log.getContainerId()))
                        .collect(Collectors.toList());
            }

            // Populate table
            ObservableList<LogEntry> logList = FXCollections.observableArrayList(logs);
            logsTable.setItems(logList);

            // Log action
            service.addLog(currentUser.getUser(), "VIEW_LOGS", null, containerId,
                    "Viewed logs: " + logs.size() + " entries");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Loading Logs",
                    "Failed to load logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show User Management panel
     */
    @FXML
    private void showUserManagement(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("user_account.fxml"));
            Parent root = loader.load();

            UserAccountController controller = loader.getController();
            controller.initialise(currentUser);
            controller.showUserManagement(null);

            Stage stage = (Stage) btnUserManagementMenu.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to load User Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show File Management panel
     */
    @FXML
    private void showFileManagement(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("user_account.fxml"));
            Parent root = loader.load();

            UserAccountController controller = loader.getController();
            controller.initialise(currentUser);
            controller.showFileManagement(null);

            Stage stage = (Stage) btnFileManagementMenu.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to load File Management: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show Advanced Panel (refresh)
     */
    @FXML
    private void showAdvancedPanel(ActionEvent event) {
        // Already on advanced panel, just highlight button
        btnAdvancedPanelMenu.setStyle(
                "-fx-background-color: #5FA4B8; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-border-width: 0;");
    }

    /**
     * Handle logout
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            service.addLog(currentUser.getUser(), "LOGOUT", null, null, "Logged out");

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogout.getScene().getWindow();
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.setTitle("File Management System - Login");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Logout Error",
                    "Failed to logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
