package com.example.bootstarter.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractionService {

    public Path extractToTemp(byte[] zipBytes) throws IOException {
        Path root = Files.createTempDirectory("spring-initializr-");
        Path extractDir = root.resolve("unzipped");
        Files.createDirectories(extractDir);

        try (InputStream in = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = extractDir.resolve(entry.getName()).normalize();

                if (!target.startsWith(extractDir)) {
                    throw new IOException("Unsafe zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        return extractDir;
    }
}

