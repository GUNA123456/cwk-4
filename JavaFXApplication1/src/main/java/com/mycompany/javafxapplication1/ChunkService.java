/*
 * ChunkService.java
 * 
 * Utility service for checking file chunk existence
 * Used by shell emulation for chunk-check command
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ChunkService - Provides file chunk checking utilities
 * 
 * @author ntu-user
 */
public class ChunkService {

    /**
     * Check for chunk files based on a base filename
     * Looks for files like: baseName_chunk1, baseName_chunk2, etc.
     * 
     * @param directory Directory to search in
     * @param baseName  Base filename (without chunk suffix)
     * @return List of chunk filenames found
     * @throws IOException if directory cannot be read
     */
    public static List<String> checkChunks(Path directory, String baseName) throws IOException {
        List<String> chunks = new ArrayList<>();

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IOException("Invalid directory: " + directory);
        }

        // Search for files matching pattern: baseName_chunk*
        String chunkPattern = baseName + "_chunk";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                if (fileName.startsWith(chunkPattern)) {
                    chunks.add(fileName);
                }
            }
        }

        // Sort chunks naturally
        chunks.sort(String::compareTo);

        return chunks;
    }

    /**
     * Count number of chunks for a base file
     * 
     * @param directory Directory to search in
     * @param baseName  Base filename
     * @return Number of chunks found
     * @throws IOException if directory cannot be read
     */
    public static int countChunks(Path directory, String baseName) throws IOException {
        return checkChunks(directory, baseName).size();
    }

    /**
     * Check if specific chunk exists
     * 
     * @param directory   Directory to search in
     * @param baseName    Base filename
     * @param chunkNumber Chunk number (1-based)
     * @return true if chunk exists
     */
    public static boolean chunkExists(Path directory, String baseName, int chunkNumber) {
        String chunkFileName = baseName + "_chunk" + chunkNumber;
        Path chunkPath = directory.resolve(chunkFileName);
        return Files.exists(chunkPath);
    }

    /**
     * Load metadata JSON file
     * 
     * @param metaFile Path to .meta.json file
     * @return JsonObject containing metadata
     * @throws IOException if file cannot be read or parsed
     */
    public static com.google.gson.JsonObject loadMetadata(Path metaFile) throws IOException {
        if (!Files.exists(metaFile)) {
            throw new IOException("Metadata file not found: " + metaFile);
        }

        String json = Files.readString(metaFile);
        return com.google.gson.JsonParser.parseString(json).getAsJsonObject();
    }

    /**
     * Check which chunks exist based on metadata
     * 
     * @param directory Directory containing chunks
     * @param metadata  Metadata JSON object
     * @return Map of chunk name to existence status
     */
    public static java.util.Map<String, Boolean> checkChunksExist(Path directory, com.google.gson.JsonObject metadata) {
        java.util.Map<String, Boolean> results = new java.util.LinkedHashMap<>();

        if (!metadata.has("chunks")) {
            return results;
        }

        com.google.gson.JsonArray chunks = metadata.get("chunks").getAsJsonArray();

        for (com.google.gson.JsonElement element : chunks) {
            com.google.gson.JsonObject chunk = element.getAsJsonObject();
            String chunkName = chunk.get("name").getAsString();
            Path chunkPath = directory.resolve(chunkName);
            results.put(chunkName, Files.exists(chunkPath));
        }

        return results;
    }

    /**
     * Compute CRC32 checksum for a chunk file
     * 
     * @param chunkFile Path to chunk file
     * @return CRC32 checksum as long
     * @throws IOException if file cannot be read
     */
    public static long computeChunkCRC(Path chunkFile) throws IOException {
        if (!Files.exists(chunkFile)) {
            throw new IOException("Chunk file not found: " + chunkFile);
        }

        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        byte[] buffer = new byte[8192];

        try (java.io.InputStream in = Files.newInputStream(chunkFile)) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }

        return crc.getValue();
    }

    /**
     * Create metadata JSON file for chunked files
     * 
     * @param workspace   Path to workspace directory
     * @param filename    Base filename
     * @param chunkResult ChunkResult containing chunk information
     * @param owner       Owner username
     * @throws IOException if metadata creation fails
     */
    public static void createMetadataFile(Path workspace, String filename,
            ChunkResult chunkResult, String owner) throws IOException {
        if (workspace == null || filename == null || chunkResult == null || owner == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        // Build JSON manually (simple approach)
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"totalChunks\": ").append(chunkResult.getTotalChunks()).append(",\n");
        json.append("  \"chunks\": [\n");

        // Add chunk information
        for (int i = 0; i < chunkResult.getChunks().size(); i++) {
            ChunkResult.ChunkInfo chunk = chunkResult.getChunks().get(i);
            json.append("    {\n");
            json.append("      \"name\": \"").append(chunk.getName()).append("\",\n");
            json.append("      \"crc32\": \"").append(chunk.getCrc32()).append("\"\n");
            json.append("    }");

            if (i < chunkResult.getChunks().size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"owner\": \"").append(owner).append("\",\n");
        json.append("  \"allowedUsers\": []\n");
        json.append("}\n");

        // Write to file
        Path metadataPath = workspace.resolve(filename + ".meta.json");
        Files.writeString(metadataPath, json.toString());
    }
}
