/*
 * MigrationService.java
 * 
 * Handles safe, idempotent database schema migrations for the JavaFX application.
 * This service ensures that schema changes (new tables, new columns) are applied
 * without dropping existing data.
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MigrationService performs database schema migrations in a safe, idempotent
 * manner.
 * 
 * Key principles:
 * - Never drop existing tables (no data loss)
 * - Use CREATE TABLE IF NOT EXISTS for new tables
 * - Use ALTER TABLE ADD COLUMN with try-catch for adding columns (idempotent)
 * - All migrations can be run multiple times safely
 * 
 * @author ntu-user
 */
public class MigrationService {

    private final DB db;

    /**
     * Constructor
     * 
     * @param db The DB instance to use for database connections
     */
    public MigrationService(DB db) {
        this.db = db;
    }

    /**
     * Execute all database migrations.
     * This method is idempotent and safe to run multiple times.
     * 
     * @throws SQLException if a critical migration error occurs
     */
    public void migrate() throws SQLException {
        System.out.println("[MigrationService] Starting database migrations...");

        try (Connection conn = db.getConnection();
                Statement stmt = conn.createStatement()) {

            // Set query timeout
            stmt.setQueryTimeout(30);

            // Enable foreign keys (best practice for SQLite)
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Migration 1: Ensure Users table exists with base schema
            createUsersTableIfNotExists(stmt);

            // Migration 2: Add 'role' column to Users table
            addRoleColumnToUsers(stmt);

            // Migration 3: Create Files table
            createFilesTable(stmt);

            // Migration 4: Create Logs table
            createLogsTable(stmt);

            System.out.println("[MigrationService] All migrations completed successfully.");

        } catch (SQLException ex) {
            System.err.println("[MigrationService] Migration failed: " + ex.getMessage());
            ex.printStackTrace();
            throw ex; // Re-throw to let caller decide how to handle
        }

        // Migration 5: Create default admin user (after tables are ready)
        createDefaultAdminUser();
    }

    /**
     * Migration 1: Create Users table if it doesn't exist.
     * This ensures the base Users table structure is present.
     */
    private void createUsersTableIfNotExists(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS Users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "password TEXT NOT NULL)";

        stmt.execute(sql);
        System.out.println("[Migration] Users table ensured to exist.");
    }

    /**
     * Migration 2: Add 'role' column to Users table.
     * Uses try-catch to handle the case where column already exists.
     * This makes the migration idempotent.
     */
    private void addRoleColumnToUsers(Statement stmt) {
        String sql = "ALTER TABLE Users ADD COLUMN role TEXT DEFAULT 'user'";

        try {
            stmt.execute(sql);
            System.out.println("[Migration] Added 'role' column to Users table.");
        } catch (SQLException e) {
            // Column likely already exists - this is expected on subsequent runs
            // SQLite error code for "duplicate column name" is caught here
            if (e.getMessage().contains("duplicate column name") ||
                    e.getMessage().contains("already exists")) {
                System.out.println("[Migration] 'role' column already exists in Users table (skipped).");
            } else {
                // Unexpected error - log it but don't fail the entire migration
                System.err.println("[Migration] Warning: Could not add 'role' column: " + e.getMessage());
            }
        }
    }

    /**
     * Migration 3: Create Files table if it doesn't exist.
     * 
     * Schema:
     * - id: Primary key
     * - name: File name
     * - path: File path
     * - owner: Username of the file owner
     * - allowed_users: CSV or JSON string of users with access
     * - created_at: Timestamp of file creation
     */
    private void createFilesTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS Files (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "path TEXT NOT NULL, " +
                "owner TEXT NOT NULL, " +
                "allowed_users TEXT, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP)";

        stmt.execute(sql);
        System.out.println("[Migration] Files table created/verified.");
    }

    /**
     * Migration 4: Create Logs table if it doesn't exist.
     * 
     * Schema:
     * - id: Primary key
     * - timestamp: When the action occurred
     * - username: User who performed the action
     * - action: Type of action (e.g., "LOGIN", "FILE_UPLOAD", "FILE_DELETE")
     * - target_file: File affected by the action (if applicable)
     * - container_id: Container ID (for distributed system context)
     * - details: Additional details in JSON or text format
     */
    private void createLogsTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS Logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "username TEXT, " +
                "action TEXT, " +
                "target_file TEXT, " +
                "container_id TEXT, " +
                "details TEXT)";

        stmt.execute(sql);
        System.out.println("[Migration] Logs table created/verified.");
    }

    /**
     * Migration 5: Create default admin user if it doesn't exist.
     * 
     * Creates a default admin account with:
     * - Username: admin
     * - Password: admin123
     * - Role: admin
     * 
     * This is only created if no user named "admin" exists.
     * For production, this password should be changed immediately.
     */
    private void createDefaultAdminUser() {
        try {
            DBService service = new DBService();

            // Check if admin user already exists
            User existingAdmin = service.getUser("admin");

            if (existingAdmin == null) {
                // Create default admin user
                String defaultPassword = "admin123";
                String hashedPassword = db.generateSecurePassword(defaultPassword);

                service.addUser("admin", hashedPassword, "admin");
                service.addLog("system", "CREATE_ADMIN", null, null,
                        "Default admin user created during migration");

                System.out.println("[Migration] ✅ Default admin user created:");
                System.out.println("            Username: admin");
                System.out.println("            Password: admin123");
                System.out.println("            ⚠️  IMPORTANT: Change this password after first login!");
            } else {
                System.out.println("[Migration] Admin user already exists (skipped).");
            }

        } catch (Exception e) {
            // Don't fail the entire migration if admin creation fails
            System.err.println("[Migration] Warning: Could not create default admin user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
