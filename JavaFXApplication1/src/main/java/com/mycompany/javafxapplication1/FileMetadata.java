/*
 * FileMetadata.java
 * 
 * Model class representing file metadata in the system.
 * Contains file information including name, path, owner, and access control.
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.util.ArrayList;
import java.util.List;

/**
 * FileMetadata model class (POJO)
 * Represents metadata for files stored in the system
 * 
 * @author ntu-user
 */
public class FileMetadata {

    private int id;
    private String name;
    private String path;
    private String owner;
    private List<String> allowedUsers;
    private String createdAt;

    /**
     * Default constructor
     */
    public FileMetadata() {
        this.allowedUsers = new ArrayList<>();
    }

    /**
     * Constructor with all fields
     * 
     * @param id           File ID
     * @param name         File name
     * @param path         File path
     * @param owner        Owner username
     * @param allowedUsers List of usernames with access
     * @param createdAt    Creation timestamp
     */
    public FileMetadata(int id, String name, String path, String owner,
            List<String> allowedUsers, String createdAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.owner = owner;
        this.allowedUsers = allowedUsers != null ? allowedUsers : new ArrayList<>();
        this.createdAt = createdAt;
    }

    /**
     * Constructor without ID (for new files before DB insert)
     * 
     * @param name         File name
     * @param path         File path
     * @param owner        Owner username
     * @param allowedUsers List of usernames with access
     */
    public FileMetadata(String name, String path, String owner, List<String> allowedUsers) {
        this(0, name, path, owner, allowedUsers, null);
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(List<String> allowedUsers) {
        this.allowedUsers = allowedUsers != null ? allowedUsers : new ArrayList<>();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", owner='" + owner + '\'' +
                ", allowedUsers=" + allowedUsers +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
