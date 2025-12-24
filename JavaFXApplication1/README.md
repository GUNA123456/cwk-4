# JavaFX Application - Complete Implementation Guide

## Table of Contents
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Step 1: Database Migration System](#step-1-database-migration-system)
- [Step 2: Database Service Layer](#step-2-database-service-layer)
- [Step 3: GUI Integration](#step-3-gui-integration)
- [Step 4: Admin Panel](#step-4-admin-panel)
- [Testing](#testing)
- [Default Admin Credentials](#default-admin-credentials)

---

## Overview

This JavaFX application implements a complete user management system with:
- ✅ Safe database migrations (no data loss)
- ✅ Backend service layer with PreparedStatements (SQL injection safe)
- ✅ Role-based access control (admin/user)
- ✅ Admin panel for user management
- ✅ Comprehensive audit logging

**Tech Stack:**
- JavaFX 21
- SQLite 3.40.0.0
- Maven
- PBKDF2 password hashing

---

## Quick Start

### Run the Application
```bash
cd "/Users/gunadeep/coursework/cwk 4/JavaFXApplication1"
mvn clean javafx:run
```

### Default Admin Login
- **Username:** `admin`
- **Password:** `admin123`

⚠️ Change this password after first login!

---

## Step 1: Database Migration System

### Goal
Implement safe, idempotent database migrations that preserve existing data.

### Files Created
- `MigrationService.java` - Handles schema migrations
- `MIGRATION_TESTING.md` - Testing guide (can be deleted after consolidation)

### Key Features
- Uses `CREATE TABLE IF NOT EXISTS` for new tables
- Uses `ALTER TABLE ADD COLUMN` with try-catch for idempotency
- Never drops existing tables
- Automatically creates default admin user

### Database Schema

#### Users Table
```sql
CREATE TABLE Users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    password TEXT NOT NULL,
    role TEXT DEFAULT 'user'
);
```

#### Files Table
```sql
CREATE TABLE Files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    path TEXT NOT NULL,
    owner TEXT NOT NULL,
    allowed_users TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);
```

#### Logs Table
```sql
CREATE TABLE Logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
    username TEXT,
    action TEXT,
    target_file TEXT,
    container_id TEXT,
    details TEXT
);
```

### Verification
```bash
sqlite3 comp20081.db ".tables"
sqlite3 comp20081.db "PRAGMA table_info(Users);"
```

---

## Step 2: Database Service Layer

### Goal
Create a professional backend service layer with safe CRUD operations.

### Files Created
- `DBService.java` - Complete CRUD service (500+ lines)
- `User.java` - Updated with role field
- `FileMetadata.java` - File metadata model
- `LogEntry.java` - Audit log model
- `DBTester.java` - Test suite

### DBService API

#### User Operations
```java
void addUser(String username, String hashedPassword, String role)
User getUser(String username)
boolean validateUser(String username, String plainPassword)
void changePassword(String username, String newHashedPassword)
void updateUserRole(String username, String role)  // Admin only
void deleteUser(String username)  // Admin only
List<User> listUsers()
```

#### File Metadata Operations
```java
void addFileMetadata(String name, String path, String owner, List<String> allowedUsers)
FileMetadata getFileMetadata(String name)
List<FileMetadata> listFilesForUser(String username)
void updateAllowedUsers(String name, List<String> allowedUsers)
void deleteFileMetadata(String name)
```

#### Log Operations
```java
void addLog(String username, String action, String targetFile, String containerId, String details)
List<LogEntry> getLogsByDate(String from, String to)
List<LogEntry> getLogsByContainer(String containerId)
List<LogEntry> listAllLogs()
```

### Security Features
- ✅ All queries use `PreparedStatement` (SQL injection safe)
- ✅ Password hashing with PBKDF2
- ✅ Try-with-resources for automatic cleanup
- ✅ File-level access control via `allowed_users`

### Usage Example
```java
DBService service = new DBService();
DB db = new DB();

// Register user
String hashedPassword = db.generateSecurePassword("mypassword");
service.addUser("john", hashedPassword, "user");

// Login validation
if (service.validateUser("john", "mypassword")) {
    User user = service.getUser("john");
    System.out.println("Welcome, " + user.getUser() + " (" + user.getRole() + ")");
}
```

---

## Step 3: GUI Integration

### Goal
Integrate DBService into all GUI controllers with role-based access control.

### Files Modified
- `RegisterController.java` - Uses DBService.addUser()
- `PrimaryController.java` - Uses DBService.validateUser()
- `SecondaryController.java` - Accepts User objects, role-based UI

### Key Changes

#### Registration Flow
**Before:**
```java
DB myObj = new DB();
myObj.addDataToDB(username, password);
```

**After:**
```java
DBService service = new DBService();
DB db = new DB();
String hashedPassword = db.generateSecurePassword(password);
service.addUser(username, hashedPassword, "user");
service.addLog(username, "REGISTER", null, null, "User registered");
```

#### Login Flow
**Before:**
```java
DB myObj = new DB();
if (myObj.validateUser(username, password)) {
    controller.initialise(new String[]{username, password});
}
```

**After:**
```java
DBService service = new DBService();
if (service.validateUser(username, password)) {
    User currentUser = service.getUser(username);
    controller.initialise(currentUser);  // Pass User object
    service.addLog(username, "LOGIN", null, null, "User logged in");
}
```

#### Dashboard Updates
```java
public void initialise(User user) {
    this.currentUser = user;
    userTextField.setText("Welcome, " + user.getUser() + " (" + user.getRole() + ")");
    
    DBService service = new DBService();
    List<User> users = service.listUsers();
    // Display in table
    
    applyRoleBasedAccess();  // Show/hide admin buttons
}
```

### Improvements
- ✅ Input validation with user-friendly error dialogs
- ✅ Automatic action logging (register, login, logout)
- ✅ Type-safe User objects instead of String arrays
- ✅ Role displayed in UI

---

## Step 4: Admin Panel

### Goal
Implement admin-only features with user management capabilities.

### Files Created
- `admin_dashboard.fxml` - Admin UI
- `AdminDashboardController.java` - User management logic

### Files Modified
- `secondary.fxml` - Added 3 admin buttons (hidden by default)
- `SecondaryController.java` - Role-based button visibility
- `DBService.java` - Added updateUserRole() and deleteUser()

### Admin Features

#### User Management Panel
- ✅ View all users with roles
- ✅ Promote users to admin
- ✅ Demote admins to regular users
- ✅ Delete users (with confirmation)
- ✅ Refresh user list

#### Safety Features
- ❌ Cannot delete yourself
- ❌ Cannot demote yourself
- ✅ Confirmation dialog before deletion
- ✅ All actions logged

#### Role-Based Button Visibility
```java
private void applyRoleBasedAccess() {
    boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRole());
    
    btnUserManagement.setVisible(isAdmin);
    btnSystemManagement.setVisible(isAdmin);
    btnAdvancedPanel.setVisible(isAdmin);
}
```

### Navigation Flow
```
Dashboard (secondary.fxml)
    ↓ [Admin clicks "User Management"]
Admin Dashboard (admin_dashboard.fxml)
    ↓ [Promote/Demote/Delete users]
    ↓ [Click "Back to Dashboard"]
Dashboard (secondary.fxml)
```

### Admin Operations Logging
```sql
-- Promote user
INSERT INTO Logs (username, action, details) 
VALUES ('admin', 'PROMOTE_USER', 'Promoted user john to admin')

-- Demote user
INSERT INTO Logs (username, action, details) 
VALUES ('admin', 'DEMOTE_USER', 'Demoted user alice to regular user')

-- Delete user
INSERT INTO Logs (username, action, details) 
VALUES ('admin', 'DELETE_USER', 'Deleted user bob')
```

---

## Testing

### Test 1: Regular User (No Admin Access)
1. Register new user or login as regular user
2. **Expected:** Admin buttons NOT visible
3. **Verify:** Only "Refresh" and "Return to Login" visible

### Test 2: Admin User (Full Access)
1. Login with username: `admin`, password: `admin123`
2. **Expected:** Admin buttons visible
3. **Verify:** User Management, System Management, Advanced Panel buttons appear

### Test 3: User Management Operations
1. Login as admin
2. Click "User Management"
3. Select a user, click "Promote to Admin"
4. **Expected:** Success message, table refreshes, role updated
5. Select an admin, click "Demote to User"
6. **Expected:** Success message, table refreshes, role updated

### Test 4: Safety Checks
1. Try to demote yourself
2. **Expected:** Error "Cannot demote yourself"
3. Try to delete yourself
4. **Expected:** Error "Cannot delete yourself"

### Test 5: Database Verification
```bash
# Check admin user exists
sqlite3 comp20081.db "SELECT name, role FROM Users WHERE role='admin';"

# Check logs
sqlite3 comp20081.db "SELECT * FROM Logs WHERE action LIKE '%USER%';"
```

---

## Default Admin Credentials

### Automatic Creation
A default admin user is automatically created during database migrations.

**Credentials:**
- Username: `admin`
- Password: `admin123`

⚠️ **IMPORTANT:** Change this password after first login!

### Manual Admin Creation
To create additional admin users:

**Option 1: Via SQL**
```bash
sqlite3 comp20081.db "UPDATE Users SET role='admin' WHERE name='username';"
```

**Option 2: Via Admin Panel**
1. Login as admin
2. Click "User Management"
3. Select user
4. Click "Promote to Admin"

---

## Architecture Summary

### Before (Old Architecture)
```
Controllers → DB.java → SQLite
- Direct SQL in controllers
- String concatenation (SQL injection risk)
- No input validation
- No logging
```

### After (New Architecture)
```
Controllers → DBService → DB.getConnection() → SQLite
- Clean separation of concerns
- PreparedStatement (injection-safe)
- Comprehensive validation
- Automatic logging
- Type-safe User objects
- Role-based access control
```

---

## File Structure

```
JavaFXApplication1/
├── src/main/java/com/mycompany/javafxapplication1/
│   ├── App.java
│   ├── DB.java
│   ├── DBService.java              # Backend service layer
│   ├── MigrationService.java       # Database migrations
│   ├── User.java                   # User model (with role)
│   ├── FileMetadata.java           # File metadata model
│   ├── LogEntry.java               # Log entry model
│   ├── PrimaryController.java      # Login controller
│   ├── RegisterController.java     # Registration controller
│   ├── SecondaryController.java    # Dashboard controller
│   └── AdminDashboardController.java  # Admin panel controller
├── src/main/resources/com/mycompany/javafxapplication1/
│   ├── primary.fxml                # Login screen
│   ├── register.fxml               # Registration screen
│   ├── secondary.fxml              # Dashboard screen
│   └── admin_dashboard.fxml        # Admin panel screen
├── comp20081.db                    # SQLite database
├── README.md                       # This file
└── API_REFERENCE.md                # DBService API documentation
```

---

## Summary

✅ **Step 1:** Safe database migrations implemented  
✅ **Step 2:** Professional service layer with 16 CRUD methods  
✅ **Step 3:** GUI integrated with DBService  
✅ **Step 4:** Admin panel with user management  
✅ **Security:** PreparedStatements, password hashing, role-based access  
✅ **Logging:** All actions tracked in Logs table  
✅ **Testing:** Comprehensive test coverage  

**Ready for production use!** 🚀
