package com.projectsa.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Files live outside the database under app.upload-dir; tables keep the relative path (CLAUDE.md §4).
 * Only profile pictures are public (/uploads/profile/**). Project documents and requirement attachments
 * are sent only through UC03's download endpoints after the access check.
 */
@Component
public class FileStore implements WebMvcConfigurer {

    private final Path root;

    public FileStore(@Value("${app.upload-dir}") String dir) throws IOException {
        this.root = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("profile"));
    }

    /** Saves the file under <root>/<folder>/<uuid>.<ext> and returns "<folder>/<uuid>.<ext>". */
    public String save(MultipartFile file, String folder) {
        String name = UUID.randomUUID() + "." + FileCheck.extension(file.getOriginalFilename());
        Path target = root.resolve(folder).resolve(name).normalize();
        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ชื่อไฟล์ไม่ถูกต้อง");
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("cannot save upload", e);
        }
        return folder + "/" + name;
    }

    /**
     * Like save(), but keeps the original name after the uuid ("<folder>/<uuid>-<name>"), for tables that store
     * only a path (REQUIREMENT_ATTACHMENT.AttachmentFile). displayName() gives the original name back.
     */
    public String saveNamed(MultipartFile file, String folder) {
        String original = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String safe = original.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path target = root.resolve(folder).resolve(UUID.randomUUID() + "-" + safe).normalize();
        if (!target.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ชื่อไฟล์ไม่ถูกต้อง");
        }
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("cannot save upload", e);
        }
        return root.relativize(target).toString().replace('\\', '/');
    }

    public static String displayName(String storedPath) {
        String name = storedPath.substring(storedPath.lastIndexOf('/') + 1);
        return name.length() > 37 && name.charAt(36) == '-' ? name.substring(37) : name;
    }

    /** A stored file for download; 404 when it is missing on disk. */
    public Resource load(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) {
            throw ApiException.notFound("ไม่พบไฟล์");
        }
        return new FileSystemResource(p);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profile/**").addResourceLocations(root.resolve("profile").toUri().toString());
    }
}
