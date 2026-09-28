package com.realestate.api.listing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Saves uploaded listing photos and hands back the public URL they're served at.
 *
 * <p>By default this writes to a local folder (app.upload-dir, served back at /uploads/** by
 * WebConfig) - fine for one server, but Render's free tier wipes local disk on every
 * restart/redeploy. If app.cloudinary.cloud-name and upload-preset are both set, photos go to
 * Cloudinary's free tier instead (an unsigned upload, since this call is already server-side -
 * see DEPLOY.md), which survives restarts and needs no extra infrastructure.
 */
@Service
public class PhotoStorageService {

    private static final Map<String, String> ALLOWED_TYPES =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final long MAX_BYTES = 3L * 1024 * 1024; // 3MB per photo
    static final int MAX_PHOTOS_PER_LISTING = 5;

    private final Path root;
    private final String cloudinaryCloudName;
    private final String cloudinaryUploadPreset;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;

    public PhotoStorageService(
            @Value("${app.upload-dir}") String uploadDir,
            @Value("${app.cloudinary.cloud-name:}") String cloudinaryCloudName,
            @Value("${app.cloudinary.upload-preset:}") String cloudinaryUploadPreset,
            ObjectMapper objectMapper) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.cloudinaryCloudName = cloudinaryCloudName;
        this.cloudinaryUploadPreset = cloudinaryUploadPreset;
        this.objectMapper = objectMapper;
    }

    /** Validates one file and saves it. Returns its public URL. */
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

        if (usesCloudinary()) {
            return storeInCloudinary(listingId, file, extension);
        }
        return storeLocally(listingId, file, extension);
    }

    private boolean usesCloudinary() {
        return !cloudinaryCloudName.isBlank() && !cloudinaryUploadPreset.isBlank();
    }

    private String storeLocally(String listingId, MultipartFile file, String extension) {
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

    private String storeInCloudinary(String listingId, MultipartFile file, String extension) {
        String boundary = "MysuruHomes" + UUID.randomUUID();
        try {
            byte[] body = buildMultipartBody(boundary, listingId, file, extension);

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudinaryCloudName + "/image/upload"))
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                            .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new PhotoStorageException("Could not save that photo. Please try again.");
            }

            JsonNode json = objectMapper.readTree(response.body());
            String secureUrl = json.path("secure_url").asString(null);
            if (secureUrl == null) {
                throw new PhotoStorageException("Could not save that photo. Please try again.");
            }
            return secureUrl;
        } catch (IOException | JacksonException e) {
            throw new PhotoStorageException("Could not save that photo. Please try again.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PhotoStorageException("Could not save that photo. Please try again.");
        }
    }

    private byte[] buildMultipartBody(String boundary, String listingId, MultipartFile file, String extension)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeField(out, boundary, "upload_preset", cloudinaryUploadPreset);
        writeField(out, boundary, "folder", "listings/" + listingId);
        writeFilePart(out, boundary, "file", UUID.randomUUID() + "." + extension, file);
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void writeField(ByteArrayOutputStream out, String boundary, String name, String value)
            throws IOException {
        out.write(
                ("--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                                + value + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
    }

    private void writeFilePart(
            ByteArrayOutputStream out, String boundary, String fieldName, String filename, MultipartFile file)
            throws IOException {
        out.write(
                ("--" + boundary + "\r\n"
                                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename
                                + "\"\r\n"
                                + "Content-Type: " + file.getContentType() + "\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        try (InputStream in = file.getInputStream()) {
            in.transferTo(out);
        }
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
