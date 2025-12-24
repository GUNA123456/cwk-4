/*
 * DBService.java
 * 
 * Professional database service layer providing CRUD operations for Users, Files, and Logs.
 * Uses PreparedStatements for all database operations to prevent SQL injection.
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DBService - Backend database service layer
 * 
 * Provides safe, reusable CRUD operations for:
 * - Users (authentication, role management)
 * - File Metadata (file tracking, access control)
 * - Logs (audit trail, monitoring)
 * 
 * All operations use PreparedStatements to prevent SQL injection.
 * 
 * @author ntu-user
 */
public class DBService {

    private final DB db;

    /**
     * Constructor - initializes DB instance
     */
    public DBService() {
        this.db = new DB();
    }

    // ========================================================================
    // USER OPERATIONS
    // ========================================================================

    /**
     * Add a new user to the database
     * 
     * @param username       Username
     * @param hashedPassword Already-hashed password
     * @param role           User role (e.g., "user", "admin")
     * @throws SQLException if database operation fails
     */
    public void addUser(String username, String hashedPassword, String role) throws SQLException {
        String sql = "INSERT INTO Users (name, password, role) VALUES (?, ?, ?)";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, role != null ? role : "user");
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Failed to add user: " + username, e);
        }
    }

    /**
     * Get a user by username
     * 
     * @param username Username to search for
     * @return User object or null if not found
     * @throws SQLException if database operation fails
     */
    public User getUser(String username) throws SQLException {
        String sql = "SELECT name, password, role FROM Users WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String password = rs.getString("password");
                String role = rs.getString("role");
                return new User(name, password, role != null ? role : "user");
            }

            return null;

        } catch (SQLException e) {
            throw new SQLException("Failed to get user: " + username, e);
        }
    }

    /**
     * Validate user credentials
     * 
     * @param username      Username
     * @param plainPassword Plain text password (will be hashed for comparison)
     * @return true if credentials are valid, false otherwise
     */
    public boolean validateUser(String username, String plainPassword) {
        try {
            User user = getUser(username);
            if (user == null) {
                return false;
            }

            // Hash the provided plain password and compare with stored hash
            String hashedInput = db.generateSecurePassword(plainPassword);
            return user.getPass().equals(hashedInput);

        } catch (SQLException | InvalidKeySpecException e) {
            System.err.println("Error validating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Change a user's password
     * 
     * @param username          Username
     * @param newHashedPassword New hashed password
     * @throws SQLException if database operation fails
     */
    public void changePassword(String username, String newHashedPassword) throws SQLException {
        String sql = "UPDATE Users SET password = ? WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newHashedPassword);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("User not found: " + username);
            }

        } catch (SQLException e) {
            throw new SQLException("Failed to change password for user: " + username, e);
        }
    }

    /**
     * List all users in the system
     * 
     * @return List of User objects
     * @throws SQLException if database operation fails
     */
    public List<User> listUsers() throws SQLException {
        String sql = "SELECT name, password, role FROM Users";
        List<User> users = new ArrayList<>();

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                String password = rs.getString("password");
                String role = rs.getString("role");
                users.add(new User(name, password, role != null ? role : "user"));
            }

            return users;

        } catch (SQLException e) {
            throw new SQLException("Failed to list users", e);
        }
    }

    /**
     * Update a user's role (admin operation)
     * 
     * @param username Username
     * @param role     New role (e.g., "user", "admin")
     * @throws SQLException if database operation fails
     */
    public void updateUserRole(String username, String role) throws SQLException {
        String sql = "UPDATE Users SET role = ? WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("User not found: " + username);
            }

        } catch (SQLException e) {
            throw new SQLException("Failed to update role for user: " + username, e);
        }
    }

    /**
     * Delete a user from the database (admin operation)
     * 
     * @param username Username to delete
     * @throws SQLException if database operation fails
     */
    public void deleteUser(String username) throws SQLException {
        String sql = "DELETE FROM Users WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("User not found: " + username);
            }

        } catch (SQLException e) {
            throw new SQLException("Failed to delete user: " + username, e);
        }
    }

    // ========================================================================
    // FILE METADATA OPERATIONS
    // ========================================================================

    /**
     * Add file metadata to the database
     * 
     * @param name         File name
     * @param path         File path
     * @param owner        Owner username
     * @param allowedUsers List of usernames with access
     * @throws SQLException if database operation fails
     */
    public void addFileMetadata(String name, String path, String owner, List<String> allowedUsers) throws SQLException {
        String sql = "INSERT INTO Files (name, path, owner, allowed_users) VALUES (?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, path);
            pstmt.setString(3, owner);
            pstmt.setString(4, toCSV(allowedUsers));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Failed to add file metadata: " + name, e);
        }
    }

    /**
     * Get file metadata by file name
     * 
     * @param name File name
     * @return FileMetadata object or null if not found
     * @throws SQLException if database operation fails
     */
    public FileMetadata getFileMetadata(String name) throws SQLException {
        String sql = "SELECT id, name, path, owner, allowed_users, created_at FROM Files WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String fileName = rs.getString("name");
                String path = rs.getString("path");
                String owner = rs.getString("owner");
                String allowedUsersCSV = rs.getString("allowed_users");
                String createdAt = rs.getString("created_at");

                List<String> allowedUsers = parseAllowedUsers(allowedUsersCSV);
                return new FileMetadata(id, fileName, path, owner, allowedUsers, createdAt);
            }

            return null;

        } catch (SQLException e) {
            throw new SQLException("Failed to get file metadata: " + name, e);
        }
    }

    /**
     * List files accessible by a specific user
     * Returns files where user is owner OR in allowed_users list
     * 
     * @param username Username
     * @return List of FileMetadata objects
     * @throws SQLException if database operation fails
     */
    public List<FileMetadata> listFilesForUser(String username) throws SQLException {
        String sql = "SELECT id, name, path, owner, allowed_users, created_at FROM Files " +
                "WHERE owner = ? OR allowed_users LIKE ?";
        List<FileMetadata> files = new ArrayList<>();

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, "%" + username + "%"); // LIKE for CSV search
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String path = rs.getString("path");
                String owner = rs.getString("owner");
                String allowedUsersCSV = rs.getString("allowed_users");
                String createdAt = rs.getString("created_at");

                List<String> allowedUsers = parseAllowedUsers(allowedUsersCSV);

                // Double-check that user is actually in allowed list (LIKE can have false
                // positives)
                if (owner.equals(username) || allowedUsers.contains(username)) {
                    files.add(new FileMetadata(id, name, path, owner, allowedUsers, createdAt));
                }
            }

            return files;

        } catch (SQLException e) {
            throw new SQLException("Failed to list files for user: " + username, e);
        }
    }

    /**
     * Update allowed users for a file
     * 
     * @param name         File name
     * @param allowedUsers New list of allowed usernames
     * @throws SQLException if database operation fails
     */
    public void updateAllowedUsers(String name, List<String> allowedUsers) throws SQLException {
        String sql = "UPDATE Files SET allowed_users = ? WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, toCSV(allowedUsers));
            pstmt.setString(2, name);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("File not found: " + name);
            }

        } catch (SQLException e) {
            throw new SQLException("Failed to update allowed users for file: " + name, e);
        }
    }

    /**
     * Delete file metadata
     * 
     * @param name File name
     * @throws SQLException if database operation fails
     */
    public void deleteFileMetadata(String name) throws SQLException {
        String sql = "DELETE FROM Files WHERE name = ?";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("File not found: " + name);
            }

        } catch (SQLException e) {
            throw new SQLException("Failed to delete file metadata: " + name, e);
        }
    }

    // ========================================================================
    // LOG OPERATIONS
    // ========================================================================

    /**
     * Add a log entry
     * 
     * @param username    User who performed the action
     * @param action      Action type (e.g., "LOGIN", "FILE_UPLOAD")
     * @param targetFile  Target file (can be null)
     * @param containerId Container ID (can be null)
     * @param details     Additional details (can be null)
     * @throws SQLException if database operation fails
     */
    public void addLog(String username, String action, String targetFile,
            String containerId, String details) throws SQLException {
        String sql = "INSERT INTO Logs (username, action, target_file, container_id, details) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, action);
            pstmt.setString(3, targetFile);
            pstmt.setString(4, containerId);
            pstmt.setString(5, details);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new SQLException("Failed to add log entry", e);
        }
    }

    /**
     * Get logs within a date range
     * 
     * @param from Start timestamp (ISO format: YYYY-MM-DD HH:MM:SS)
     * @param to   End timestamp (ISO format: YYYY-MM-DD HH:MM:SS)
     * @return List of LogEntry objects
     * @throws SQLException if database operation fails
     */
    public List<LogEntry> getLogsByDate(String from, String to) throws SQLException {
        String sql = "SELECT id, timestamp, username, action, target_file, container_id, details " +
                "FROM Logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, from);
            pstmt.setString(2, to);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(extractLogEntry(rs));
            }

            return logs;

        } catch (SQLException e) {
            throw new SQLException("Failed to get logs by date", e);
        }
    }

    /**
     * Get logs for a specific container
     * 
     * @param containerId Container ID
     * @return List of LogEntry objects
     * @throws SQLException if database operation fails
     */
    public List<LogEntry> getLogsByContainer(String containerId) throws SQLException {
        String sql = "SELECT id, timestamp, username, action, target_file, container_id, details " +
                "FROM Logs WHERE container_id = ? ORDER BY timestamp DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, containerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(extractLogEntry(rs));
            }

            return logs;

        } catch (SQLException e) {
            throw new SQLException("Failed to get logs by container: " + containerId, e);
        }
    }

    /**
     * List all log entries
     * 
     * @return List of LogEntry objects
     * @throws SQLException if database operation fails
     */
    public List<LogEntry> listAllLogs() throws SQLException {
        String sql = "SELECT id, timestamp, username, action, target_file, container_id, details " +
                "FROM Logs ORDER BY timestamp DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = db.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(extractLogEntry(rs));
            }

            return logs;

        } catch (SQLException e) {
            throw new SQLException("Failed to list all logs", e);
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Extract LogEntry from ResultSet
     * 
     * @param rs ResultSet positioned at a log row
     * @return LogEntry object
     * @throws SQLException if extraction fails
     */
    private LogEntry extractLogEntry(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String timestamp = rs.getString("timestamp");
        String username = rs.getString("username");
        String action = rs.getString("action");
        String targetFile = rs.getString("target_file");
        String containerId = rs.getString("container_id");
        String details = rs.getString("details");

        return new LogEntry(id, timestamp, username, action, targetFile, containerId, details);
    }

    /**
     * Parse CSV string into list of usernames
     * 
     * @param csv Comma-separated string
     * @return List of usernames (empty list if csv is null or empty)
     */
    private List<String> parseAllowedUsers(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Convert list of usernames to CSV string
     * 
     * @param list List of usernames
     * @return Comma-separated string (empty string if list is null or empty)
     */
    private String toCSV(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }

    /**
     * Safely close AutoCloseable resources
     * 
     * @param closeable Resource to close
     */
    private void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                System.err.println("Error closing resource: " + e.getMessage());
            }
        }
    }
}
