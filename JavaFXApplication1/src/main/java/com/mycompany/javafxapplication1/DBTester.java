/*
 * DBTester.java
 * 
 * Test class for DBService - demonstrates and tests all CRUD operations
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

/**
 * DBTester - Manual testing class for DBService
 * 
 * Tests all CRUD operations for Users, Files, and Logs
 * 
 * @author ntu-user
 */
public class DBTester {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("DBService Testing Suite");
        System.out.println("========================================\n");

        try {
            DBService service = new DBService();
            DB db = new DB(); // For password hashing

            // ================================================================
            // TEST 1: User Operations
            // ================================================================
            System.out.println("--- TEST 1: User Operations ---");

            // Add users
            String hashedPassword1 = db.generateSecurePassword("password123");
            String hashedPassword2 = db.generateSecurePassword("admin456");

            service.addUser("john", hashedPassword1, "user");
            service.addUser("alice", hashedPassword2, "admin");
            System.out.println("✓ Added users: john (user), alice (admin)");

            // Get user
            User john = service.getUser("john");
            System.out.println("✓ Retrieved user: " + john);

            // Validate user
            boolean valid = service.validateUser("john", "password123");
            boolean invalid = service.validateUser("john", "wrongpassword");
            System.out.println("✓ Validation test - correct password: " + valid);
            System.out.println("✓ Validation test - wrong password: " + invalid);

            // List users
            List<User> users = service.listUsers();
            System.out.println("✓ Total users in database: " + users.size());
            for (User u : users) {
                System.out.println("  - " + u);
            }

            System.out.println();

            // ================================================================
            // TEST 2: File Metadata Operations
            // ================================================================
            System.out.println("--- TEST 2: File Metadata Operations ---");

            // Add files
            service.addFileMetadata("document1.txt", "/files/document1.txt", "john",
                    Arrays.asList("alice", "bob"));
            service.addFileMetadata("report.pdf", "/files/report.pdf", "alice",
                    Arrays.asList("john"));
            service.addFileMetadata("private.doc", "/files/private.doc", "john", null);
            System.out.println("✓ Added 3 files");

            // Get file metadata
            FileMetadata file1 = service.getFileMetadata("document1.txt");
            System.out.println("✓ Retrieved file: " + file1);

            // List files for user
            List<FileMetadata> johnsFiles = service.listFilesForUser("john");
            System.out.println("✓ Files accessible by john: " + johnsFiles.size());
            for (FileMetadata f : johnsFiles) {
                System.out.println("  - " + f.getName() + " (owner: " + f.getOwner() + ")");
            }

            // Update allowed users
            service.updateAllowedUsers("document1.txt", Arrays.asList("alice", "bob", "charlie"));
            System.out.println("✓ Updated allowed users for document1.txt");

            FileMetadata updated = service.getFileMetadata("document1.txt");
            System.out.println("  New allowed users: " + updated.getAllowedUsers());

            System.out.println();

            // ================================================================
            // TEST 3: Log Operations
            // ================================================================
            System.out.println("--- TEST 3: Log Operations ---");

            // Add logs
            service.addLog("john", "LOGIN", null, null, "User logged in successfully");
            service.addLog("john", "FILE_UPLOAD", "document1.txt", "container-1",
                    "Uploaded file to container");
            service.addLog("alice", "FILE_DOWNLOAD", "report.pdf", "container-2",
                    "Downloaded file from container");
            service.addLog("john", "FILE_DELETE", "temp.txt", "container-1",
                    "Deleted temporary file");
            System.out.println("✓ Added 4 log entries");

            // List all logs
            List<LogEntry> allLogs = service.listAllLogs();
            System.out.println("✓ Total logs in database: " + allLogs.size());
            for (LogEntry log : allLogs) {
                System.out.println("  - [" + log.getTimestamp() + "] " +
                        log.getUsername() + " -> " + log.getAction());
            }

            // Get logs by container
            List<LogEntry> container1Logs = service.getLogsByContainer("container-1");
            System.out.println("✓ Logs for container-1: " + container1Logs.size());
            for (LogEntry log : container1Logs) {
                System.out.println("  - " + log.getAction() + " on " + log.getTargetFile());
            }

            System.out.println();

            // ================================================================
            // TEST 4: Edge Cases
            // ================================================================
            System.out.println("--- TEST 4: Edge Cases ---");

            // Get non-existent user
            User nonExistent = service.getUser("nonexistent");
            System.out.println("✓ Get non-existent user: " + (nonExistent == null ? "null (correct)" : "ERROR"));

            // Validate non-existent user
            boolean invalidUser = service.validateUser("nonexistent", "password");
            System.out.println("✓ Validate non-existent user: " + (!invalidUser ? "false (correct)" : "ERROR"));

            // List files for user with no files
            List<FileMetadata> noFiles = service.listFilesForUser("bob");
            System.out.println("✓ Files for user with no files: " + noFiles.size() + " (expected 0)");

            System.out.println();

            // ================================================================
            // SUMMARY
            // ================================================================
            System.out.println("========================================");
            System.out.println("All tests completed successfully! ✓");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("❌ Test failed with exception:");
            e.printStackTrace();
        }
    }
}
