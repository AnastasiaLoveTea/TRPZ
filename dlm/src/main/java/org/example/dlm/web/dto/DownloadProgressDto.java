package org.example.dlm.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DownloadProgressDto(
        UUID id,
        long receivedBytes,
        long totalBytes,
        String status,
        double avgSpeedBps,
        double maxSpeedBps,
        Instant createdAt,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        int retries
) {}
