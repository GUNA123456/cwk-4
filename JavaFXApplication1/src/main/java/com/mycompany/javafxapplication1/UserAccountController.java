/*
 * UserAccountController.java
 * 
 * Controller for User Account Panel with Sidebar Navigation
 * - User Management: Password update and account deletion
 * - File Management: Create, update, delete, upload, download files
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * UserAccountController - Handles user self-management and file operations
 */
public class UserAccountController {

    private User currentUser;
    private DBService service;

    // Panels
    @FXML
    private AnchorPane userManagementPanel;

    @FXML
    private AnchorPane fileManagementPanel;

    @FXML
    private AnchorPane shellPanel;

    // Sidebar buttons
    @FXML
    private Button btnUserManagementMenu;

    @FXML
    private Button btnFileManagementMenu;

    @FXML
    private Button btnAdvancedPanelMenu;

    // User Management fields
    @FXML
    private Label lblUser;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField retypePasswordField;

    @FXML
    private Button updatePasswordBtn;

    @FXML
    private Button deleteAccountBtn;

    @FXML
    private Button backBtn;

    @FXML
    private Label lblStatus;

    // File Management fields
    @FXML
    private TextField filePathField;

    @FXML
    private TextField fileNameField;

    @FXML
    private TextArea fileContentArea;

    @FXML
    private Button btnCreate;

    @FXML
    private Button btnUpdate;

    @FXML
    private Button btnDelete;

    @FXML
    private Button btnShell;

    @FXML
    private Button btnUpload;

    @FXML
    private Button btnBrowseFilePath;

    @FXML
    private Button btnBrowseDownloadPath;

    @FXML
    private Button btnClear;

    @FXML
    private ListView<String> fileListView;

    @FXML
    private TextField downloadPathField;

    @FXML
    private Button btnDownload;

    @FXML
    private Label lblFileStatus;

    // Shell Emulator fields
    @FXML
    private Label lblShellInfo;

    @FXML
    private TextArea txtShellOutput;

    @FXML
    private TextField txtShellCommand;

    @FXML
    private Button btnShellRun;

    @FXML
    private Button btnShellClear;

    @FXML
    private Button btnShellHistory;

    @FXML
    private Button btnShellBack;

    // Shell logic
    private java.util.List<String> commandHistory;
    private int historyIndex;
    private java.nio.file.Path workspace;

    /**
     * Initialize the panel with the current user
     */
    public void initialise(User user) {
        this.currentUser = user;
        this.service = new DBService();

        if (user != null) {
            lblUser.setText("User: " + user.getUser());
        }

        // Show User Management panel by default
        showUserManagement(null);

        // Load file list
        loadFileList();

        // Add double-click listener to file list
        setupFileListListener();
    }

    /**
     * Setup double-click listener for file list
     */
    private void setupFileListListener() {
        fileListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedFile = fileListView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    loadSelectedFile(selectedFile);
                }
            }
        });
    }

    /**
     * Show User Management panel
     */
    @FXML
    public void showUserManagement(ActionEvent event) {
        userManagementPanel.setVisible(true);
        fileManagementPanel.setVisible(false);
        shellPanel.setVisible(false); // Hide shell panel

        // Highlight selected menu (exact colors)
        btnUserManagementMenu.setStyle(
                "-fx-background-color: #5FA4B8; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-border-width: 0;");
        btnFileManagementMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-border-width: 0;");
    }

    /**
     * Show File Management panel
     */
    @FXML
    public void showFileManagement(ActionEvent event) {
        userManagementPanel.setVisible(false);
        fileManagementPanel.setVisible(true);
        shellPanel.setVisible(false); // Hide shell panel

        // Highlight selected menu (exact colors)
        btnFileManagementMenu.setStyle(
                "-fx-background-color: #5FA4B8; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-border-width: 0;");
        btnUserManagementMenu.setStyle(
                "-fx-background-color: #1A3A52; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-border-width: 0;");

        loadFileList();
    }

    /**
     * Show Advanced Panel
     */
    @FXML
    public void showAdvancedPanel(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("advanced_panel.fxml"));
            Parent root = loader.load();

            AdvancedPanelController controller = loader.getController();
            controller.initialise(currentUser);

            Stage stage = (Stage) btnAdvancedPanelMenu.getScene().getWindow();
            stage.getScene().setRoot(root);

            // Log access
            try {
                service.addLog(currentUser.getUser(), "ADVANCED_PANEL_ACCESS", null, null,
                        "Accessed Advanced Panel");
            } catch (Exception logEx) {
                // Log error but don't block navigation
                logEx.printStackTrace();
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to load Advanced Panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================================================
    // USER MANAGEMENT OPERATIONS
    // ========================================================================

    /**
     * Handle update password
     */
    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user logged in.");
            return;
        }

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String retypePassword = retypePasswordField.getText();

        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Current password cannot be empty!");
            return;
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "New password cannot be empty!");
            return;
        }

        if (retypePassword == null || retypePassword.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please retype your new password!");
            return;
        }

        if (!service.validateUser(currentUser.getUser(), currentPassword)) {
            showAlert(Alert.AlertType.ERROR, "Wrong Password", "Current password is incorrect!");
            currentPasswordField.clear();
            return;
        }

        if (!newPassword.equals(retypePassword)) {
            showAlert(Alert.AlertType.ERROR, "Password Mismatch", "New passwords do not match!");
            newPasswordField.clear();
            retypePasswordField.clear();
            return;
        }

        try {
            DB db = new DB();
            String hashedPassword = db.generateSecurePassword(newPassword);

            service.changePassword(currentUser.getUser(), hashedPassword);
            service.addLog(currentUser.getUser(), "UPDATE_PASSWORD", currentUser.getUser(), null,
                    "Password updated successfully");

            showAlert(Alert.AlertType.INFORMATION, "Success", "Password updated successfully!");

            currentPasswordField.clear();
            newPasswordField.clear();
            retypePasswordField.clear();
            lblStatus.setText("Password updated at " + java.time.LocalTime.now());

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Failed to update password: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle delete account
     */
    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No user logged in.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Account Deletion");
        confirm.setHeaderText("Delete Your Account");
        confirm.setContentText("Are you sure you want to delete your account?\\n\\nThis action CANNOT be undone!");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String username = currentUser.getUser();
                    service.addLog(username, "DELETE_ACCOUNT", username, null, "User deleted own account");
                    service.deleteUser(username);

                    showAlert(Alert.AlertType.INFORMATION, "Account Deleted",
                            "Your account has been deleted successfully.");
                    navigateToLogin();

                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Failed to delete account: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // ========================================================================
    // FILE MANAGEMENT OPERATIONS
    // ========================================================================

    /**
     * Load file list from database
     */
    private void loadFileList() {
        try {
            var files = service.listFilesForUser(currentUser.getUser());
            var fileNames = files.stream().map(FileMetadata::getName).collect(Collectors.toList());
            fileListView.setItems(FXCollections.observableArrayList(fileNames));
        } catch (Exception e) {
            System.err.println("Error loading file list: " + e.getMessage());
        }
    }

    /**
     * Handle create file
     */
    @FXML
    private void handleCreateFile(ActionEvent event) {
        String fileName = fileNameField.getText();
        String filePath = filePathField.getText();
        String content = fileContentArea.getText();

        if (fileName == null || fileName.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "File name cannot be empty!");
            return;
        }

        try {
            // Create file on disk
            File file = new File(filePath.isEmpty() ? fileName : filePath);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }

            // Add to database
            service.addFileMetadata(fileName, file.getAbsolutePath(), currentUser.getUser(),
                    java.util.Arrays.asList(currentUser.getUser()));
            service.addLog(currentUser.getUser(), "FILE_CREATE", fileName, null, "Created file: " + fileName);

            showAlert(Alert.AlertType.INFORMATION, "Success", "File created successfully!");
            loadFileList();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Creation Failed", "Failed to create file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle update file
     */
    @FXML
    private void handleUpdateFile(ActionEvent event) {
        String fileName = fileNameField.getText();
        String content = fileContentArea.getText();

        if (fileName == null || fileName.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "File name cannot be empty!");
            return;
        }

        try {
            FileMetadata metadata = service.getFileMetadata(fileName);
            if (metadata == null) {
                showAlert(Alert.AlertType.ERROR, "Not Found", "File not found!");
                return;
            }

            File file = new File(metadata.getPath());
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }

            service.addLog(currentUser.getUser(), "FILE_UPDATE", fileName, null, "Updated file: " + fileName);
            showAlert(Alert.AlertType.INFORMATION, "Success", "File updated successfully!");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Failed to update file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle delete file
     */
    @FXML
    private void handleDeleteFile(ActionEvent event) {
        String fileName = fileNameField.getText();

        if (fileName == null || fileName.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "File name cannot be empty!");
            return;
        }

        try {
            service.deleteFileMetadata(fileName);
            service.addLog(currentUser.getUser(), "FILE_DELETE", fileName, null, "Deleted file: " + fileName);

            showAlert(Alert.AlertType.INFORMATION, "Success", "File deleted successfully!");
            loadFileList();
            fileNameField.clear();
            fileContentArea.clear();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Failed to delete file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle shell emulation - Show shell panel inline
     */
    @FXML
    private void handleShellEmulation(ActionEvent event) {
        try {
            // Initialize shell if first time
            if (commandHistory == null) {
                commandHistory = new java.util.ArrayList<>();
                historyIndex = -1;
                workspace = FileService.getUserWorkspace(currentUser.getUser());
                FileService.ensureWorkspaceExists(currentUser.getUser());

                // Setup Enter key listener
                txtShellCommand.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        handleShellRunCommand(null);
                    } else if (keyEvent.getCode() == javafx.scene.input.KeyCode.UP) {
                        if (!commandHistory.isEmpty() && historyIndex < commandHistory.size() - 1) {
                            historyIndex++;
                            txtShellCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
                        }
                    } else if (keyEvent.getCode() == javafx.scene.input.KeyCode.DOWN) {
                        if (historyIndex > 0) {
                            historyIndex--;
                            txtShellCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
                        } else if (historyIndex == 0) {
                            historyIndex = -1;
                            txtShellCommand.clear();
                        }
                    }
                });

                // Welcome message
                txtShellOutput.clear();
                appendShellOutput("╔════════════════════════════════════════════════════════╗");
                appendShellOutput("║          SHELL EMULATOR - RESTRICTED MODE              ║");
                appendShellOutput("╚════════════════════════════════════════════════════════╝");
                appendShellOutput("");
                appendShellOutput("User: " + currentUser.getUser());
                appendShellOutput("Workspace: " + workspace.toString());
                appendShellOutput("");
                appendShellOutput("Type 'help' for available commands");
                appendShellOutput("═══════════════════════════════════════════════════════════");
                appendShellOutput("");
            }

            // Show shell panel
            userManagementPanel.setVisible(false);
            fileManagementPanel.setVisible(false);
            shellPanel.setVisible(true);

            // Focus on command field
            txtShellCommand.requestFocus();

            service.addLog(currentUser.getUser(), "SHELL_OPEN", null, null,
                    "Opened shell emulator");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Shell Error",
                    "Failed to open shell emulator: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle shell run command
     */
    @FXML
    private void handleShellRunCommand(ActionEvent event) {
        String command = txtShellCommand.getText().trim();

        if (command.isEmpty()) {
            return;
        }

        // Add to history
        commandHistory.add(command);
        historyIndex = -1;

        // Display command
        appendShellOutput("> " + command);

        // Parse and execute
        parseShellCommand(command);

        // Log command
        try {
            service.addLog(currentUser.getUser(), "SHELL_COMMAND", null, null,
                    "Executed: " + command);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Clear input
        txtShellCommand.clear();

        // Add blank line
        appendShellOutput("");
    }

    /**
     * Parse and execute shell command
     */
    private void parseShellCommand(String input) {
        String[] parts = input.trim().split("\\s+");

        if (parts.length == 0) {
            return;
        }

        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "ls":
                    if (parts.length > 1 && "-l".equals(parts[1])) {
                        cmd_ls_long();
                    } else {
                        cmd_ls();
                    }
                    break;

                case "cat":
                    if (parts.length < 2) {
                        appendShellOutput("Error: cat requires a filename");
                        appendShellOutput("Usage: cat <filename>");
                    } else {
                        cmd_cat(parts[1]);
                    }
                    break;

                case "pwd":
                    cmd_pwd();
                    break;

                case "crc32":
                    if (parts.length < 2) {
                        appendShellOutput("Error: crc32 requires a filename");
                        appendShellOutput("Usage: crc32 <filename>");
                    } else {
                        cmd_crc32(parts[1]);
                    }
                    break;

                case "chunk-check":
                case "chunkcheck":
                    if (parts.length < 2) {
                        appendShellOutput("Error: chunk-check requires a filename");
                        appendShellOutput("Usage: chunk-check <filename>");
                    } else {
                        cmd_chunk_check(parts[1]);
                    }
                    break;

                case "help":
                    cmd_help();
                    break;

                case "clear":
                    txtShellOutput.clear();
                    break;

                default:
                    appendShellOutput("Unknown command: " + cmd);
                    appendShellOutput("Type 'help' for available commands");
            }
        } catch (Exception e) {
            appendShellOutput("Error executing command: " + e.getMessage());
        }
    }

    /**
     * Command: ls - List files from database
     */
    private void cmd_ls() {
        try {
            // Get files from database (same as File Management panel)
            java.util.List<FileMetadata> fileMetadataList = service.listFilesForUser(currentUser.getUser());

            if (fileMetadataList.isEmpty()) {
                appendShellOutput("(no files)");
            } else {
                // Sort by filename
                fileMetadataList.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));

                for (FileMetadata metadata : fileMetadataList) {
                    appendShellOutput("  " + metadata.getName());
                }
            }

        } catch (Exception e) {
            appendShellOutput("Error listing files: " + e.getMessage());
        }
    }

    /**
     * Command: ls -l - List files with details from database
     */
    private void cmd_ls_long() {
        try {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // Get files from database
            java.util.List<FileMetadata> fileMetadataList = service.listFilesForUser(currentUser.getUser());

            if (fileMetadataList.isEmpty()) {
                appendShellOutput("(no files)");
                return;
            }

            appendShellOutput(String.format("%-10s %-20s %s", "SIZE", "MODIFIED", "NAME"));
            appendShellOutput("─────────────────────────────────────────────────────");

            // Sort by filename
            fileMetadataList.sort((f1, f2) -> f1.getName().compareTo(f2.getName()));

            for (FileMetadata metadata : fileMetadataList) {
                java.io.File file = new java.io.File(metadata.getPath());
                long size = file.exists() ? file.length() : 0;
                String modified = file.exists()
                        ? dateFormat.format(new java.util.Date(file.lastModified()))
                        : "N/A";
                String name = metadata.getName();

                appendShellOutput(String.format("%-10d %-20s %s", size, modified, name));
            }

        } catch (Exception e) {
            appendShellOutput("Error listing files: " + e.getMessage());
        }
    }

    /**
     * Command: cat <filename> - Display file contents
     */
    private void cmd_cat(String filename) {
        try {
            java.nio.file.Path filePath = workspace.resolve(filename);

            if (!java.nio.file.Files.exists(filePath)) {
                appendShellOutput("Error: File not found: " + filename);
                return;
            }

            if (!java.nio.file.Files.isRegularFile(filePath)) {
                appendShellOutput("Error: Not a regular file: " + filename);
                return;
            }

            String content = FileService.readFile(filePath);
            appendShellOutput("─── " + filename + " ───");
            appendShellOutput(content);
            appendShellOutput("─── End of file ───");

        } catch (IOException e) {
            appendShellOutput("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Command: pwd - Print working directory
     */
    private void cmd_pwd() {
        appendShellOutput(workspace.toString());
    }

    /**
     * Command: crc32 <filename> - Calculate CRC32 checksum
     */
    private void cmd_crc32(String filename) {
        try {
            java.nio.file.Path filePath = workspace.resolve(filename);

            if (!java.nio.file.Files.exists(filePath)) {
                appendShellOutput("Error: File not found: " + filename);
                return;
            }

            String checksum = ChecksumService.calculateCRC32(filePath);
            appendShellOutput("CRC32 (" + filename + "): " + checksum);

        } catch (IOException e) {
            appendShellOutput("Error calculating checksum: " + e.getMessage());
        }
    }

    /**
     * Command: chunk-check <filename> - Check for file chunks
     */
    private void cmd_chunk_check(String filename) {
        try {
            java.util.List<String> chunks = ChunkService.checkChunks(workspace, filename);

            if (chunks.isEmpty()) {
                appendShellOutput("No chunks found for: " + filename);
            } else {
                appendShellOutput("Found " + chunks.size() + " chunk(s) for: " + filename);
                for (String chunk : chunks) {
                    appendShellOutput("  ✓ " + chunk);
                }
            }

        } catch (IOException e) {
            appendShellOutput("Error checking chunks: " + e.getMessage());
        }
    }

    /**
     * Command: help - Show available commands
     */
    private void cmd_help() {
        appendShellOutput("═══════════════════════════════════════════════════════════");
        appendShellOutput("                    AVAILABLE COMMANDS                     ");
        appendShellOutput("═══════════════════════════════════════════════════════════");
        appendShellOutput("");
        appendShellOutput("  ls                  - List files in workspace");
        appendShellOutput("  ls -l               - List files with size and date");
        appendShellOutput("  cat <file>          - Display file contents");
        appendShellOutput("  pwd                 - Show current workspace path");
        appendShellOutput("  crc32 <file>        - Calculate CRC32 checksum");
        appendShellOutput("  chunk-check <file>  - Check for file chunks");
        appendShellOutput("  history             - Show command history");
        appendShellOutput("  clear               - Clear output window");
        appendShellOutput("  help                - Show this help message");
        appendShellOutput("");
        appendShellOutput("═══════════════════════════════════════════════════════════");
        appendShellOutput("");
        appendShellOutput("Note: All commands run in your workspace directory only.");
        appendShellOutput("You cannot access files outside your workspace.");
    }

    /**
     * Handle shell clear button
     */
    @FXML
    private void handleShellClear(ActionEvent event) {
        txtShellOutput.clear();
        appendShellOutput("Output cleared.");
        appendShellOutput("");
    }

    /**
     * Handle shell history button
     */
    @FXML
    private void handleShellHistory(ActionEvent event) {
        if (commandHistory.isEmpty()) {
            appendShellOutput("No command history.");
        } else {
            appendShellOutput("Command History:");
            appendShellOutput("─────────────────────────────────────────────────────");
            for (int i = 0; i < commandHistory.size(); i++) {
                appendShellOutput(String.format("%3d  %s", i + 1, commandHistory.get(i)));
            }
        }
        appendShellOutput("");
    }

    /**
     * Handle shell back button - return to file management
     */
    @FXML
    private void handleShellBack(ActionEvent event) {
        try {
            service.addLog(currentUser.getUser(), "SHELL_CLOSE", null, null,
                    "Closed shell emulator");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Show file management panel
        showFileManagement(null);
    }

    /**
     * Append text to shell output
     */
    private void appendShellOutput(String text) {
        txtShellOutput.appendText(text + "\n");
    }

    /**
     * Handle upload file with automatic chunking (ALL files are chunked)
     */
    @FXML
    private void handleUpload(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        File file = fileChooser.showOpenDialog(btnUpload.getScene().getWindow());

        if (file != null) {
            try {
                // Get file info
                String filename = file.getName();
                Path sourcePath = file.toPath();

                // Get user workspace
                Path workspace = FileService.ensureWorkspaceExists(currentUser.getUser());

                // Chunk the file (ALL files are chunked, regardless of size)
                ChunkResult chunkResult = FileService.chunkFile(sourcePath, workspace, filename);

                // Create metadata JSON
                ChunkService.createMetadataFile(workspace, filename, chunkResult, currentUser.getUser());

                // Save to database with chunk info
                String metadataPath = workspace.resolve(filename + ".meta.json").toString();
                service.addFileMetadata(filename, metadataPath, currentUser.getUser(), null);

                // Log the upload
                service.addLog(currentUser.getUser(), "FILE_UPLOAD_CHUNKED", null, filename,
                        "Uploaded and chunked into " + chunkResult.getTotalChunks() + " chunks");

                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "File uploaded and split into " + chunkResult.getTotalChunks() + " chunks!");

                // Update file path field
                filePathField.setText(file.getAbsolutePath());
                fileNameField.setText(filename);

                // Refresh file list
                loadFileList();

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Upload Failed",
                        "Failed to upload file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle download file
     */
    @FXML
    private void handleDownload(ActionEvent event) {
        String fileName = fileListView.getSelectionModel().getSelectedItem();

        if (fileName == null) {
            showAlert(Alert.AlertType.ERROR, "No Selection", "Please select a file from the list!");
            return;
        }

        try {
            FileMetadata metadata = service.getFileMetadata(fileName);
            if (metadata == null) {
                showAlert(Alert.AlertType.ERROR, "Not Found", "File not found!");
                return;
            }

            File sourceFile = new File(metadata.getPath());
            String content = Files.readString(sourceFile.toPath());

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");
            fileChooser.setInitialFileName(fileName);
            File destFile = fileChooser.showSaveDialog(btnDownload.getScene().getWindow());

            if (destFile != null) {
                try (FileWriter writer = new FileWriter(destFile)) {
                    writer.write(content);
                }

                service.addLog(currentUser.getUser(), "FILE_DOWNLOAD", fileName, null, "Downloaded file: " + fileName);
                showAlert(Alert.AlertType.INFORMATION, "Success", "File downloaded successfully!");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Download Failed", "Failed to download file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================================================
    // NAVIGATION
    // ========================================================================

    /**
     * Handle back to dashboard (logout)
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            if (currentUser != null) {
                service.addLog(currentUser.getUser(), "LOGOUT", null, null, "User logged out");
            }

            Stage currentStage = (Stage) backBtn.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 640, 480);
            currentStage.setScene(scene);
            currentStage.setTitle("Login");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to logout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to login screen
     */
    private void navigateToLogin() {
        try {
            Stage currentStage = (Stage) backBtn.getScene().getWindow();
            Stage newStage = new Stage();

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 640, 480);
            newStage.setScene(scene);
            newStage.setTitle("Login");
            newStage.show();
            currentStage.close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========================================================================
    // NEW ENHANCED FILE MANAGEMENT METHODS
    // ========================================================================

    /**
     * Handle browse file path - opens FileChooser to select file
     */
    @FXML
    private void handleBrowseFilePath(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        // Set initial directory to user's home directory
        File userHome = new File(System.getProperty("user.home"));
        if (userHome.exists()) {
            fileChooser.setInitialDirectory(userHome);
        }

        // Use showOpenDialog for selecting existing files (no "Replace?" popup)
        File file = fileChooser.showOpenDialog(btnBrowseFilePath.getScene().getWindow());
        if (file != null) {
            // Always update both path and filename
            filePathField.setText(file.getAbsolutePath());
            fileNameField.setText(file.getName());

            // Load file content if file exists
            if (file.exists()) {
                try {
                    String content = FileService.readFile(file.toPath());
                    fileContentArea.setText(content);
                    setStatusMessage("Loaded: " + file.getName(), false);
                } catch (Exception e) {
                    setStatusMessage("Failed to load file content: " + e.getMessage(), true);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Handle browse download path - opens DirectoryChooser to select download
     * folder
     */
    @FXML
    private void handleBrowseDownloadPath(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Download Folder");

        File dir = dirChooser.showDialog(btnBrowseDownloadPath.getScene().getWindow());
        if (dir != null) {
            downloadPathField.setText(dir.getAbsolutePath());
        }
    }

    /**
     * Handle clear form - reset all file management fields
     */
    @FXML
    private void handleClearForm(ActionEvent event) {
        filePathField.clear();
        fileNameField.clear();
        fileContentArea.clear();
        downloadPathField.clear();
        fileListView.getSelectionModel().clearSelection();
        setStatusMessage("Form cleared", false);
    }

    /**
     * Load selected file from list into editor
     */
    private void loadSelectedFile(String fileName) {
        try {
            FileMetadata metadata = service.getFileMetadata(fileName);
            if (metadata == null) {
                setStatusMessage("File not found in database", true);
                return;
            }

            // Read file content using FileService
            String content = FileService.readFile(java.nio.file.Paths.get(metadata.getPath()));

            // Populate fields
            fileNameField.setText(fileName);
            filePathField.setText(metadata.getPath());
            fileContentArea.setText(content);

            setStatusMessage("Loaded: " + fileName, false);

        } catch (Exception e) {
            setStatusMessage("Failed to load file: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    /**
     * Set status message with color coding
     * 
     * @param message Message to display
     * @param isError true for error (red), false for success (green)
     */
    private void setStatusMessage(String message, boolean isError) {
        if (lblFileStatus != null) {
            lblFileStatus.setText(message);
            if (isError) {
                lblFileStatus.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 13px; -fx-font-weight: bold;");
            } else {
                lblFileStatus.setStyle("-fx-text-fill: #28a745; -fx-font-size: 13px; -fx-font-weight: bold;");
            }
        }
    }

    /**
     * 
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
