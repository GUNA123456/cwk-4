/*
 * ChunkResult.java
 * 
 * Data class to hold information about file chunking results
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.util.ArrayList;
import java.util.List;

/**
 * ChunkResult - Holds information about chunked files
 * 
 * @author ntu-user
 */
public class ChunkResult {
    private int totalChunks;
    private List<ChunkInfo> chunks;
    private String originalFilename;
    private long originalFileSize;

    public ChunkResult(String originalFilename, long originalFileSize) {
        this.originalFilename = originalFilename;
        this.originalFileSize = originalFileSize;
        this.chunks = new ArrayList<>();
        this.totalChunks = 0;
    }

    public void addChunk(ChunkInfo chunk) {
        this.chunks.add(chunk);
        this.totalChunks++;
    }

    // Getters
    public int getTotalChunks() {
        return totalChunks;
    }

    public List<ChunkInfo> getChunks() {
        return chunks;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getOriginalFileSize() {
        return originalFileSize;
    }

    /**
     * ChunkInfo - Information about a single chunk
     */
    public static class ChunkInfo {
        private String name;
        private String crc32;
        private long size;

        public ChunkInfo(String name, String crc32, long size) {
            this.name = name;
            this.crc32 = crc32;
            this.size = size;
        }

        // Getters
        public String getName() {
            return name;
        }

        public String getCrc32() {
            return crc32;
        }

        public long getSize() {
            return size;
        }
    }
}
