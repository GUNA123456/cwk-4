/*
 * FileService.java
 * 
 * Utility class for centralized file operations
 * Provides reusable methods for create, read, update, delete, and copy operations
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * FileService - Centralized file operations utility
 */
public class FileService {

    // Chunking configuration
    public static final int CHUNK_SIZE = 64 * 1024; // 64KB chunks (fixed size for all files)

    /**
     * Create a new file with the given content
     * 
     * @param path    Path to the file
     * @param content Content to write
     * @throws IOException if file creation fails
     */
    public static void createFile(Path path, String content) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        // Create parent directories if they don't exist
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // Write content to file
        Files.writeString(path, content != null ? content : "");
    }

    /**
     * Update an existing file with new content
     * 
     * @param path    Path to the file
     * @param content New content to write
     * @throws IOException if file update fails
     */
    public static void updateFile(Path path, String content) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        // Overwrite file with new content
        Files.writeString(path, content != null ? content : "");
    }

    /**
     * Delete a file from the filesystem
     * 
     * @param path Path to the file
     * @throws IOException if file deletion fails
     */
    public static void deleteFile(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        Files.delete(path);
    }

    /**
     * Read file content as a String
     * 
     * @param path Path to the file
     * @return File content as String
     * @throws IOException if file reading fails
     */
    public static String readFile(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        return Files.readString(path);
    }

    /**
     * Copy a file from source to destination
     * 
     * @param source      Source file path
     * @param destination Destination file path
     * @throws IOException if file copy fails
     */
    public static void copyFile(Path source, Path destination) throws IOException {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Source and destination cannot be null");
        }

        if (!Files.exists(source)) {
            throw new IOException("Source file does not exist: " + source);
        }

        // Create parent directories if they don't exist
        Path parent = destination.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // Copy file, replacing if exists
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Check if a file exists
     * 
     * @param path Path to check
     * @return true if file exists, false otherwise
     */
    public static boolean fileExists(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * Get file size in bytes
     * 
     * @param path Path to the file
     * @return File size in bytes
     * @throws IOException if unable to get file size
     */
    public static long getFileSize(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return 0;
        }
        return Files.size(path);
    }

    /**
     * Validate file name (no special characters, not empty)
     * 
     * @param fileName File name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        // Check for invalid characters: \ / : * ? " < > |
        String invalidChars = "\\/:*?\"<>|";
        for (char c : invalidChars.toCharArray()) {
            if (fileName.indexOf(c) >= 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get user workspace directory
     * 
     * @param username Username
     * @return Path to user's workspace
     */
    public static Path getUserWorkspace(String username) {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "workspace", username);
    }

    /**
     * Ensure user workspace exists
     * 
     * @param username Username
     * @return Path to user's workspace
     * @throws IOException if workspace creation fails
     */
    public static Path ensureWorkspaceExists(String username) throws IOException {
        Path workspace = getUserWorkspace(username);
        if (!Files.exists(workspace)) {
            Files.createDirectories(workspace);
        }
        return workspace;
    }

    /**
     * Split a file into chunks and calculate CRC32 for each chunk
     * 
     * @param sourceFile Path to the source file
     * @param workspace  Path to workspace directory
     * @param filename   Base filename for chunks
     * @return ChunkResult containing chunk information
     * @throws IOException if chunking fails
     */
    public static ChunkResult chunkFile(Path sourceFile, Path workspace, String filename) throws IOException {
        if (sourceFile == null || workspace == null || filename == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        if (!Files.exists(sourceFile)) {
            throw new IOException("Source file does not exist: " + sourceFile);
        }

        // Ensure workspace exists
        if (!Files.exists(workspace)) {
            Files.createDirectories(workspace);
        }

        long fileSize = Files.size(sourceFile);
        ChunkResult result = new ChunkResult(filename, fileSize);

        // Read entire file
        byte[] fileData = Files.readAllBytes(sourceFile);

        // Calculate number of chunks
        int numChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

        // Split into chunks
        for (int i = 0; i < numChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileData.length);
            byte[] chunkData = new byte[end - start];
            System.arraycopy(fileData, start, chunkData, 0, end - start);

            // Create chunk file
            String chunkName = filename + ".chunk" + (i + 1);
            Path chunkPath = workspace.resolve(chunkName);
            Files.write(chunkPath, chunkData);

            // Calculate CRC32
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(chunkData);
            long crcValue = crc.getValue();
            String crcHex = Long.toHexString(crcValue).toUpperCase();

            // Pad with zeros to 8 digits
            while (crcHex.length() < 8) {
                crcHex = "0" + crcHex;
            }

            // Add chunk info to result
            result.addChunk(new ChunkResult.ChunkInfo(chunkName, crcHex, chunkData.length));
        }

        return result;
    }
}
