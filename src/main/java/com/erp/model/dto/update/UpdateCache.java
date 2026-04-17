package com.erp.model.dto.update;

public record UpdateCache(
        String lastCheckedAt,
        UpdateManifest lastManifest,
        String lastError
) {
}
