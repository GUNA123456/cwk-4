/*
 * AccessService.java
 * 
 * Service for checking file access permissions based on metadata
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * AccessService - Handles file access permission checking
 * 
 * @author ntu-user
 */
public class AccessService {

    /**
     * Check if user has access to a file based on metadata
     * 
     * @param user     Current user
     * @param metadata File metadata JSON object
     * @return true if user has access, false otherwise
     */
    public static boolean hasAccess(User user, JsonObject metadata) {
        if (user == null || metadata == null) {
            return false;
        }

        // Admin has access to everything
        if ("admin".equals(user.getRole())) {
            return true;
        }

        // Check if user is the owner
        if (isOwner(user, metadata)) {
            return true;
        }

        // Check if user is in allowed users list
        if (isInAllowedUsers(user, metadata)) {
            return true;
        }

        return false;
    }

    /**
     * Check if user is the owner of the file
     * 
     * @param user     Current user
     * @param metadata File metadata JSON object
     * @return true if user is owner
     */
    public static boolean isOwner(User user, JsonObject metadata) {
        if (user == null || metadata == null) {
            return false;
        }

        if (!metadata.has("owner")) {
            return false;
        }

        String owner = metadata.get("owner").getAsString();
        return user.getUser().equals(owner);
    }

    /**
     * Check if user is in the allowed users list
     * 
     * @param user     Current user
     * @param metadata File metadata JSON object
     * @return true if user is in allowed users list
     */
    public static boolean isInAllowedUsers(User user, JsonObject metadata) {
        if (user == null || metadata == null) {
            return false;
        }

        if (!metadata.has("allowedUsers")) {
            return false;
        }

        JsonArray allowedUsers = metadata.get("allowedUsers").getAsJsonArray();
        String username = user.getUser();

        for (JsonElement element : allowedUsers) {
            if (element.getAsString().equals(username)) {
                return true;
            }
        }

        return false;
    }
}
