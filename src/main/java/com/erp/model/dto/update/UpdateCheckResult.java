package com.erp.model.dto.update;

public record UpdateCheckResult(
        String currentVersion,
        UpdateManifest manifest
) {
}
