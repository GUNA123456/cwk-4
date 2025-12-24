# DBService API Reference

Complete API documentation for the database service layer.

---

## Overview

`DBService` provides safe, reusable CRUD operations for Users, Files, and Logs using PreparedStatements to prevent SQL injection.

**Package:** `com.mycompany.javafxapplication1`

---

## User Operations

### addUser()
```java
public void addUser(String username, String hashedPassword, String role) throws SQLException
```
Add a new user to the database.

**Parameters:**
- `username` - Username
- `hashedPassword` - Already-hashed password (use `DB.generateSecurePassword()`)
- `role` - User role (`"user"` or `"admin"`)

**Throws:** `SQLException` if user already exists or database error

**Example:**
```java
DBService service = new DBService();
DB db = new DB();
String hashed = db.generateSecurePassword("password123");
service.addUser("john", hashed, "user");
```

---

### getUser()
```java
public User getUser(String username) throws SQLException
```
Retrieve a user by username.

**Returns:** `User` object or `null` if not found

**Example:**
```java
User user = service.getUser("john");
if (user != null) {
    System.out.println("Role: " + user.getRole());
}
```

---

### validateUser()
```java
public boolean validateUser(String username, String plainPassword)
```
Validate user credentials (hashes password internally).

**Parameters:**
- `username` - Username
- `plainPassword` - Plain text password (will be hashed for comparison)

**Returns:** `true` if credentials valid, `false` otherwise

**Example:**
```java
if (service.validateUser("john", "password123")) {
    // Login successful
}
```

---

### changePassword()
```java
public void changePassword(String username, String newHashedPassword) throws SQLException
```
Change a user's password.

**Throws:** `SQLException` if user not found

---

### updateUserRole() 🔒 Admin Only
```java
public void updateUserRole(String username, String role) throws SQLException
```
Update a user's role.

**Parameters:**
- `username` - Username to update
- `role` - New role (`"user"` or `"admin"`)

**Example:**
```java
service.updateUserRole("john", "admin");  // Promote to admin
service.addLog("admin", "PROMOTE_USER", null, null, "Promoted john");
```

---

### deleteUser() 🔒 Admin Only
```java
public void deleteUser(String username) throws SQLException
```
Delete a user from the database.

**Throws:** `SQLException` if user not found

**Example:**
```java
service.deleteUser("john");
service.addLog("admin", "DELETE_USER", null, null, "Deleted john");
```

---

### listUsers()
```java
public List<User> listUsers() throws SQLException
```
List all users in the system.

**Returns:** `List<User>` of all users

**Example:**
```java
List<User> users = service.listUsers();
for (User u : users) {
    System.out.println(u.getUser() + " - " + u.getRole());
}
```

---

## File Metadata Operations

### addFileMetadata()
```java
public void addFileMetadata(String name, String path, String owner, List<String> allowedUsers) throws SQLException
```
Add file metadata with access control.

**Parameters:**
- `name` - File name
- `path` - File path
- `owner` - Owner username
- `allowedUsers` - List of usernames with access

**Example:**
```java
service.addFileMetadata("report.pdf", "/files/report.pdf", "john", 
                        Arrays.asList("alice", "bob"));
```

---

### getFileMetadata()
```java
public FileMetadata getFileMetadata(String name) throws SQLException
```
Get file metadata by name.

**Returns:** `FileMetadata` object or `null`

---

### listFilesForUser()
```java
public List<FileMetadata> listFilesForUser(String username) throws SQLException
```
List files accessible by a user (owner OR in allowed_users).

**Example:**
```java
List<FileMetadata> files = service.listFilesForUser("john");
for (FileMetadata f : files) {
    System.out.println(f.getName() + " (owner: " + f.getOwner() + ")");
}
```

---

### updateAllowedUsers()
```java
public void updateAllowedUsers(String name, List<String> allowedUsers) throws SQLException
```
Update the list of users with access to a file.

---

### deleteFileMetadata()
```java
public void deleteFileMetadata(String name) throws SQLException
```
Delete file metadata.

---

## Log Operations

### addLog()
```java
public void addLog(String username, String action, String targetFile, 
                   String containerId, String details) throws SQLException
```
Add an audit log entry.

**Parameters:**
- `username` - User who performed the action
- `action` - Action type (e.g., `"LOGIN"`, `"FILE_UPLOAD"`, `"PROMOTE_USER"`)
- `targetFile` - Target file (can be `null`)
- `containerId` - Container ID (can be `null`)
- `details` - Additional details (can be `null`)

**Example:**
```java
service.addLog("john", "LOGIN", null, null, "User logged in successfully");
service.addLog("admin", "PROMOTE_USER", null, null, "Promoted john to admin");
```

---

### getLogsByDate()
```java
public List<LogEntry> getLogsByDate(String from, String to) throws SQLException
```
Get logs within a date range.

**Parameters:**
- `from` - Start timestamp (ISO format: `YYYY-MM-DD HH:MM:SS`)
- `to` - End timestamp

---

### getLogsByContainer()
```java
public List<LogEntry> getLogsByContainer(String containerId) throws SQLException
```
Get logs for a specific container.

---

### listAllLogs()
```java
public List<LogEntry> listAllLogs() throws SQLException
```
List all log entries (ordered by timestamp DESC).

**Example:**
```java
List<LogEntry> logs = service.listAllLogs();
for (LogEntry log : logs) {
    System.out.println("[" + log.getTimestamp() + "] " + 
                       log.getUsername() + " -> " + log.getAction());
}
```

---

## Model Classes

### User
```java
public class User {
    private SimpleStringProperty user;
    private SimpleStringProperty pass;
    private SimpleStringProperty role;
    
    public String getUser()
    public String getPass()
    public String getRole()
}
```

### FileMetadata
```java
public class FileMetadata {
    private int id;
    private String name;
    private String path;
    private String owner;
    private List<String> allowedUsers;
    private String createdAt;
}
```

### LogEntry
```java
public class LogEntry {
    private int id;
    private String timestamp;
    private String username;
    private String action;
    private String targetFile;
    private String containerId;
    private String details;
}
```

---

## Common Action Types

### User Actions
- `REGISTER` - User registration
- `LOGIN` - Successful login
- `LOGIN_FAILED` - Failed login attempt
- `LOGOUT` - User logout
- `CHANGE_PASSWORD` - Password changed

### Admin Actions
- `PROMOTE_USER` - User promoted to admin
- `DEMOTE_USER` - Admin demoted to user
- `DELETE_USER` - User deleted
- `CREATE_ADMIN` - Admin user created

### File Actions
- `FILE_UPLOAD` - File uploaded
- `FILE_DOWNLOAD` - File downloaded
- `FILE_DELETE` - File deleted
- `FILE_SHARE` - File shared with users

### Navigation
- `NAVIGATE` - Screen navigation

---

## Security Notes

✅ **SQL Injection Protection:** All queries use `PreparedStatement`  
✅ **Password Security:** PBKDF2 hashing with salt  
✅ **Access Control:** File-level permissions via `allowed_users`  
✅ **Resource Management:** Try-with-resources for automatic cleanup  
✅ **Error Handling:** Meaningful exceptions with context  

---

## Complete Usage Example

```java
public class Example {
    public static void main(String[] args) {
        DBService service = new DBService();
        DB db = new DB();
        
        try {
            // Register user
            String hashed = db.generateSecurePassword("password123");
            service.addUser("john", hashed, "user");
            service.addLog("john", "REGISTER", null, null, "User registered");
            
            // Login
            if (service.validateUser("john", "password123")) {
                User user = service.getUser("john");
                service.addLog("john", "LOGIN", null, null, "Logged in");
                
                // Add file
                service.addFileMetadata("doc.txt", "/files/doc.txt", "john",
                                       Arrays.asList("alice"));
                
                // List files
                List<FileMetadata> files = service.listFilesForUser("john");
                System.out.println("Files: " + files.size());
                
                // Admin operations (if admin)
                if ("admin".equals(user.getRole())) {
                    service.updateUserRole("alice", "admin");
                    service.addLog("john", "PROMOTE_USER", null, null, 
                                  "Promoted alice");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
