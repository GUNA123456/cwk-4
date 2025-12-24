/*
 * ShellController.java
 * 
 * Controller for shell emulation interface
 * Provides safe, restricted command execution in user workspace
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * ShellController - Handles shell emulation logic
 * 
 * @author ntu-user
 */
public class ShellController {

    private User currentUser;
    private Path workspace;
    private DBService service;
    private List<String> commandHistory;
    private int historyIndex;

    @FXML
    private Label lblShellInfo;

    @FXML
    private TextArea txtOutput;

    @FXML
    private TextField txtCommand;

    @FXML
    private Button btnRun;

    @FXML
    private Button btnClear;

    @FXML
    private Button btnHistory;

    @FXML
    private Button btnClose;

    /**
     * Initialize the shell with user context
     */
    public void initialise(User user) {
        this.currentUser = user;
        this.service = new DBService();
        this.commandHistory = new ArrayList<>();
        this.historyIndex = -1;

        // Set workspace directory
        this.workspace = FileService.getUserWorkspace(user.getUser());
        try {
            FileService.ensureWorkspaceExists(user.getUser());
        } catch (IOException e) {
            appendOutput("Warning: Could not create workspace directory: " + e.getMessage());
        }

        // Update header
        lblShellInfo.setText("Shell Emulator - User: " + user.getUser());

        // Welcome message
        appendOutput("╔════════════════════════════════════════════════════════╗");
        appendOutput("║          SHELL EMULATOR - RESTRICTED MODE              ║");
        appendOutput("╚════════════════════════════════════════════════════════╝");
        appendOutput("");
        appendOutput("User: " + user.getUser());
        appendOutput("Workspace: " + workspace.toString());
        appendOutput("");
        appendOutput("Type 'help' for available commands");
        appendOutput("═══════════════════════════════════════════════════════════");
        appendOutput("");

        // Add Enter key listener to command field
        txtCommand.setOnKeyPressed(this::handleKeyPress);

        // Focus on command field
        txtCommand.requestFocus();
    }

    /**
     * Handle key press in command field (Enter to execute)
     */
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            handleRunCommand(null);
        } else if (event.getCode() == KeyCode.UP) {
            // Navigate command history up
            if (!commandHistory.isEmpty() && historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                txtCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            }
        } else if (event.getCode() == KeyCode.DOWN) {
            // Navigate command history down
            if (historyIndex > 0) {
                historyIndex--;
                txtCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            } else if (historyIndex == 0) {
                historyIndex = -1;
                txtCommand.clear();
            }
        }
    }

    /**
     * Handle Run button click or Enter key
     */
    @FXML
    private void handleRunCommand(ActionEvent event) {
        String command = txtCommand.getText().trim();

        if (command.isEmpty()) {
            return;
        }

        // Add to history
        commandHistory.add(command);
        historyIndex = -1;

        // Display command
        appendOutput("> " + command);

        // Parse and execute
        parseCommand(command);

        // Log command
        try {
            service.addLog(currentUser.getUser(), "SHELL_COMMAND", null, null,
                    "Executed: " + command);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Clear input
        txtCommand.clear();

        // Add blank line for readability
        appendOutput("");
    }

    /**
     * Parse and execute command
     */
    private void parseCommand(String input) {
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
                        appendOutput("Error: cat requires a filename");
                        appendOutput("Usage: cat <filename>");
                    } else {
                        cmd_cat(parts[1]);
                    }
                    break;

                case "pwd":
                    cmd_pwd();
                    break;

                case "crc32":
                    if (parts.length < 2) {
                        appendOutput("Error: crc32 requires a filename");
                        appendOutput("Usage: crc32 <filename>");
                    } else {
                        cmd_crc32(parts[1]);
                    }
                    break;

                case "chunk-check":
                case "chunkcheck":
                    if (parts.length < 2) {
                        appendOutput("Error: chunk-check requires a filename");
                        appendOutput("Usage: chunk-check <filename>");
                    } else {
                        cmd_chunk_check(parts[1]);
                    }
                    break;

                case "help":
                    cmd_help();
                    break;

                case "clear":
                    clearOutput();
                    break;

                default:
                    appendOutput("Unknown command: " + cmd);
                    appendOutput("Type 'help' for available commands");
            }
        } catch (Exception e) {
            appendOutput("Error executing command: " + e.getMessage());
        }
    }

    /**
     * Command: ls - List files in workspace
     */
    private void cmd_ls() {
        try {
            List<String> files = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspace)) {
                for (Path entry : stream) {
                    files.add(entry.getFileName().toString());
                }
            }

            if (files.isEmpty()) {
                appendOutput("(empty directory)");
            } else {
                files.sort(String::compareTo);
                for (String file : files) {
                    appendOutput("  " + file);
                }
            }

        } catch (IOException e) {
            appendOutput("Error listing files: " + e.getMessage());
        }
    }

    /**
     * Command: ls -l - List files with details
     */
    private void cmd_ls_long() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            appendOutput(String.format("%-10s %-20s %s", "SIZE", "MODIFIED", "NAME"));
            appendOutput("─────────────────────────────────────────────────────");

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspace)) {
                for (Path entry : stream) {
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                    long size = attrs.size();
                    String modified = dateFormat.format(new Date(attrs.lastModifiedTime().toMillis()));
                    String name = entry.getFileName().toString();

                    appendOutput(String.format("%-10d %-20s %s", size, modified, name));
                }
            }

        } catch (IOException e) {
            appendOutput("Error listing files: " + e.getMessage());
        }
    }

    /**
     * Command: cat <filename> - Display file contents
     */
    private void cmd_cat(String filename) {
        try {
            Path filePath = workspace.resolve(filename);

            if (!Files.exists(filePath)) {
                appendOutput("Error: File not found: " + filename);
                return;
            }

            if (!Files.isRegularFile(filePath)) {
                appendOutput("Error: Not a regular file: " + filename);
                return;
            }

            String content = FileService.readFile(filePath);
            appendOutput("─── " + filename + " ───");
            appendOutput(content);
            appendOutput("─── End of file ───");

        } catch (IOException e) {
            appendOutput("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Command: pwd - Print working directory
     */
    private void cmd_pwd() {
        appendOutput(workspace.toString());
    }

    /**
     * Command: crc32 <filename> - Calculate CRC32 checksum
     */
    private void cmd_crc32(String filename) {
        try {
            Path filePath = workspace.resolve(filename);

            if (!Files.exists(filePath)) {
                appendOutput("Error: File not found: " + filename);
                return;
            }

            String checksum = ChecksumService.calculateCRC32(filePath);
            appendOutput("CRC32 (" + filename + "): " + checksum);

        } catch (IOException e) {
            appendOutput("Error calculating checksum: " + e.getMessage());
        }
    }

    /**
     * Command: chunk-check <filename> - Check for file chunks
     */
    private void cmd_chunk_check(String filename) {
        try {
            List<String> chunks = ChunkService.checkChunks(workspace, filename);

            if (chunks.isEmpty()) {
                appendOutput("No chunks found for: " + filename);
            } else {
                appendOutput("Found " + chunks.size() + " chunk(s) for: " + filename);
                for (String chunk : chunks) {
                    appendOutput("  ✓ " + chunk);
                }
            }

        } catch (IOException e) {
            appendOutput("Error checking chunks: " + e.getMessage());
        }
    }

    /**
     * Command: help - Show available commands
     */
    private void cmd_help() {
        appendOutput("═══════════════════════════════════════════════════════════");
        appendOutput("                    AVAILABLE COMMANDS                     ");
        appendOutput("═══════════════════════════════════════════════════════════");
        appendOutput("");
        appendOutput("  ls                  - List files in workspace");
        appendOutput("  ls -l               - List files with size and date");
        appendOutput("  cat <file>          - Display file contents");
        appendOutput("  pwd                 - Show current workspace path");
        appendOutput("  crc32 <file>        - Calculate CRC32 checksum");
        appendOutput("  chunk-check <file>  - Check for file chunks");
        appendOutput("  history             - Show command history");
        appendOutput("  clear               - Clear output window");
        appendOutput("  help                - Show this help message");
        appendOutput("");
        appendOutput("═══════════════════════════════════════════════════════════");
        appendOutput("");
        appendOutput("Note: All commands run in your workspace directory only.");
        appendOutput("You cannot access files outside your workspace.");
    }

    /**
     * Handle Clear button - Clear output
     */
    @FXML
    private void handleClear(ActionEvent event) {
        clearOutput();
        appendOutput("Output cleared.");
        appendOutput("");
    }

    /**
     * Handle History button - Show command history
     */
    @FXML
    private void handleHistory(ActionEvent event) {
        if (commandHistory.isEmpty()) {
            appendOutput("No command history.");
        } else {
            appendOutput("Command History:");
            appendOutput("─────────────────────────────────────────────────────");
            for (int i = 0; i < commandHistory.size(); i++) {
                appendOutput(String.format("%3d  %s", i + 1, commandHistory.get(i)));
            }
        }
        appendOutput("");
    }

    /**
     * Handle Close button - Close shell window
     */
    @FXML
    private void handleClose(ActionEvent event) {
        try {
            service.addLog(currentUser.getUser(), "SHELL_CLOSE", null, null,
                    "Closed shell emulator");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    /**
     * Append text to output area
     */
    private void appendOutput(String text) {
        txtOutput.appendText(text + "\n");
    }

    /**
     * Clear output area
     */
    private void clearOutput() {
        txtOutput.clear();
    }
}
