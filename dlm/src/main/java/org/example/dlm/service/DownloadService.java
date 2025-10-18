package org.example.dlm.service;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.Download;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.repo.DownloadRepo;
import org.example.dlm.repo.SegmentRepo;
import org.example.dlm.repo.StatsRepo;
import org.example.dlm.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadRepo downloads;
    private final SegmentRepo segments;
    private final StatsRepo stats;
    private final UserRepo users;

    @Transactional(readOnly = true)
    public List<Download> listByUser(Long userId) {
        return downloads.findByOwnerId(userId);
    }

    @Transactional
    public Download addUrl(Long userId, String url) {
        validateUrl(url);

        var owner = users.findById(userId).orElseThrow();

        var d = new Download();
        d.setId(UUID.randomUUID());
        d.setUrl(url);
        d.setStatus(DownloadStatus.QUEUED);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        d.setOwner(owner);

        return downloads.save(d);
    }

    @Transactional
    public void setStatusForUser(Long userId, UUID downloadId, DownloadStatus status) {
        var d = downloads.findByIdAndOwner_Id(downloadId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Завантаження не знайдено або вам не належить"));
        d.setStatus(status);
        d.setUpdatedAt(Instant.now());
        downloads.save(d);
    }

    @Transactional
    public void deleteForUser(Long userId, UUID downloadId) {
        downloads.findByIdAndOwner_Id(downloadId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Завантаження не знайдено або вам не належить"));

        segments.deleteByDownload_Id(downloadId);
        stats.deleteById(downloadId);

        downloads.deleteById(downloadId);
    }


    private void validateUrl(String url) {
        try {
            var u = new URI(url);
            var scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("URL має бути http або https");
            }
            if (u.getHost() == null) throw new IllegalArgumentException("URL повинен містити хост");
        } catch (Exception e) {
            throw new IllegalArgumentException("Невірний URL: " + e.getMessage(), e);
        }
    }
}
