package com.microsoft.migration.assets.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Validates uploaded files against an allowlist of permitted file types to prevent
 * CWE-434: Unrestricted Upload of File with Dangerous Type.
 *
 * <p>Both the file extension (derived from the original filename) and the declared
 * MIME content-type must appear in their respective allowlists before the upload is
 * accepted.</p>
 */
public final class FileTypeValidator {

    /** Allowlist of permitted MIME content-types. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/tiff"
    );

    /** Allowlist of permitted lowercase file extensions (including the leading dot). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".gif",
            ".webp",
            ".bmp",
            ".tif",
            ".tiff"
    );

    private FileTypeValidator() {
        // Utility class – prevent instantiation.
    }

    /**
     * Returns {@code true} when the supplied file passes all type-safety checks.
     *
     * <ol>
     *   <li>The file must have a non-blank original filename.</li>
     *   <li>The file extension must be in the {@link #ALLOWED_EXTENSIONS allowlist}.</li>
     *   <li>The declared MIME content-type must be in the {@link #ALLOWED_CONTENT_TYPES allowlist}.</li>
     * </ol>
     *
     * @param file the multipart file submitted by the client
     * @return {@code true} if the file is acceptable; {@code false} otherwise
     */
    public static boolean isAllowed(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return false;
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        // Normalise: strip any parameters (e.g. "image/jpeg; charset=…")
        String normalizedContentType = contentType.split(";")[0].trim().toLowerCase();
        return ALLOWED_CONTENT_TYPES.contains(normalizedContentType);
    }

    /**
     * Returns a human-readable description of the permitted file types, suitable for
     * use in error messages shown to end-users.
     */
    public static String getAllowedTypesDescription() {
        return "JPEG, PNG, GIF, WebP, BMP, TIFF";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase();
    }
}
