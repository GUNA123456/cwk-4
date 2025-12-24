/*
 * LogEntry.java
 * 
 * Model class representing a log entry in the system.
 * Contains information about user actions, file operations, and container activities.
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

/**
 * LogEntry model class (POJO)
 * Represents a single log entry for audit and monitoring
 * 
 * @author ntu-user
 */
public class LogEntry {

    private int id;
    private String timestamp;
    private String username;
    private String action;
    private String targetFile;
    private String containerId;
    private String details;

    /**
     * Default constructor
     */
    public LogEntry() {
    }

    /**
     * Constructor with all fields
     * 
     * @param id          Log entry ID
     * @param timestamp   Timestamp of the action
     * @param username    User who performed the action
     * @param action      Action type (e.g., "LOGIN", "FILE_UPLOAD", "FILE_DELETE")
     * @param targetFile  Target file (if applicable)
     * @param containerId Container ID (for distributed system context)
     * @param details     Additional details in JSON or text format
     */
    public LogEntry(int id, String timestamp, String username, String action,
            String targetFile, String containerId, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.username = username;
        this.action = action;
        this.targetFile = targetFile;
        this.containerId = containerId;
        this.details = details;
    }

    /**
     * Constructor without ID (for new log entries before DB insert)
     * 
     * @param username    User who performed the action
     * @param action      Action type
     * @param targetFile  Target file (if applicable)
     * @param containerId Container ID
     * @param details     Additional details
     */
    public LogEntry(String username, String action, String targetFile,
            String containerId, String details) {
        this(0, null, username, action, targetFile, containerId, details);
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(String targetFile) {
        this.targetFile = targetFile;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", timestamp='" + timestamp + '\'' +
                ", username='" + username + '\'' +
                ", action='" + action + '\'' +
                ", targetFile='" + targetFile + '\'' +
                ", containerId='" + containerId + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
