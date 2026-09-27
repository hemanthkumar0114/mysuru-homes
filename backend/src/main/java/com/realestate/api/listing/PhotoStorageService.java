package com.realestate.api.listing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Saves uploaded listing photos to a local folder (app.upload-dir) and hands back the
 * public URL they're served at (see WebConfig, which maps /uploads/** to that folder).
 * A local folder is fine for one server; swap this class for one that uploads to
 * S3/Cloud Storage if this ever needs to run on more than one instance.
 */
@Service
public class PhotoStorageService {

    private static final Map<String, String> ALLOWED_TYPES =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final long MAX_BYTES = 3L * 1024 * 1024; // 3MB per photo
    static final int MAX_PHOTOS_PER_LISTING = 5;

    private final Path root;

    public PhotoStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** Validates one file and saves it under listings/{listingId}/. Returns its public URL. */
    public String store(String listingId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new PhotoStorageException("One of the selected files was empty.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new PhotoStorageException("Each photo must be 3MB or smaller.");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new PhotoStorageException("Photos must be JPEG, PNG or WebP images.");
        }

        try {
            Path dir = root.resolve("listings").resolve(listingId);
            Files.createDirectories(dir);
            // A generated name - never the browser-supplied filename - so nothing in it
            // (path separators, "..", etc.) can put the file outside this folder.
            String filename = UUID.randomUUID() + "." + extension;
            Path target = dir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/listings/" + listingId + "/" + filename;
        } catch (IOException e) {
            throw new PhotoStorageException("Could not save that photo. Please try again.");
        }
    }
}
