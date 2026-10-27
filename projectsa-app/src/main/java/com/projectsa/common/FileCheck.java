package com.projectsa.common;

import java.util.Locale;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

/**
 * <<Controller>> File Check of the sequence diagrams. One rule per use: the narrative's file limits
 * (UC02 Step 7, UC06 Step 7) and the F3 profile picture. The same rules run in the page's JS first.
 */
public enum FileCheck {

    /** UC02 Step 7.1–7.2: PDF only, ≤ 50 MB per file. */
    SOLUTION(Set.of("pdf"), 50L * 1024 * 1024, "ต้องเป็นไฟล์ PDF และขนาดไม่เกิน 50 MB"),
    /** UC06 Step 7: PDF / JPG / PNG, ≤ 50 MB per file. */
    REQUIREMENT(Set.of("pdf", "jpg", "jpeg", "png"), 50L * 1024 * 1024, "ต้องเป็นไฟล์ PDF / JPG / PNG และขนาดไม่เกิน 50 MB"),
    /** F3 Profile P.5: JPG / PNG, ≤ 5 MB. */
    PROFILE_PICTURE(Set.of("jpg", "jpeg", "png"), 5L * 1024 * 1024, "รูปโปรไฟล์ต้องเป็น JPG / PNG และขนาดไม่เกิน 5 MB");

    private final Set<String> extensions;
    private final long maxBytes;
    private final String message;

    FileCheck(Set<String> extensions, long maxBytes, String message) {
        this.extensions = extensions;
        this.maxBytes = maxBytes;
        this.message = message;
    }

    /** null when the file is acceptable, otherwise the Thai message for the user. */
    public String problem(MultipartFile f) {
        if (f == null || f.isEmpty()) {
            return "ไม่พบไฟล์";
        }
        String ext = extension(f.getOriginalFilename());
        if (!extensions.contains(ext) || f.getSize() > maxBytes) {
            return (f.getOriginalFilename() == null ? "ไฟล์" : f.getOriginalFilename()) + ": " + message;
        }
        return null;
    }

    public static String extension(String name) {
        if (name == null || name.lastIndexOf('.') < 0) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
