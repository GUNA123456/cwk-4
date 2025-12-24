/*
 * ChecksumService.java
 * 
 * Utility service for calculating file checksums
 * Used by shell emulation for CRC32 command
 * 
 * Package: com.mycompany.javafxapplication1
 */
package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

/**
 * ChecksumService - Provides checksum calculation utilities
 * 
 * @author ntu-user
 */
public class ChecksumService {

    /**
     * Calculate CRC32 checksum for a file
     * 
     * @param filePath Path to the file
     * @return CRC32 checksum as hexadecimal string
     * @throws IOException if file cannot be read
     */
    public static String calculateCRC32(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Not a regular file: " + filePath);
        }

        CRC32 crc32 = new CRC32();

        // Read file in chunks for memory efficiency
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                crc32.update(buffer, 0, bytesRead);
            }
        }

        // Return as uppercase hexadecimal string
        return String.format("%08X", crc32.getValue());
    }

    /**
     * Calculate CRC32 for a string (useful for testing)
     * 
     * @param content String content
     * @return CRC32 checksum as hexadecimal string
     */
    public static String calculateCRC32(String content) {
        CRC32 crc32 = new CRC32();
        crc32.update(content.getBytes());
        return String.format("%08X", crc32.getValue());
    }
}
