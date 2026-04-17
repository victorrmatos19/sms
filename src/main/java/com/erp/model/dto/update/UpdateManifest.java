package com.erp.model.dto.update;

public record UpdateManifest(
        String latestVersion,
        String downloadUrl,
        String sha256,
        String releaseNotes,
        boolean mandatory,
        String publishedAt
) {
}
