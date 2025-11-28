package org.example.dlm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dlm.domain.Download;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.domain.Segment;
import org.example.dlm.domain.Stats;
import org.example.dlm.iterator.DbSegmentCollection;
import org.example.dlm.iterator.SegmentIterator;
import org.example.dlm.iterator.SegmentOrder;
import org.example.dlm.repo.DownloadRepo;
import org.example.dlm.repo.SegmentRepo;
import org.example.dlm.repo.StatsRepo;
import org.example.dlm.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadRepo downloads;
    private final SegmentRepo segments;
    private final StatsRepo stats;
    private final UserRepo users;
    private final ActiveTaskRegistry activeTaskRegistry;

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

        d.setFileName(filenameFromUrl(url));
        d.setSavePath(defaultSavePath());

        Download saved = downloads.save(d);

        if (!stats.existsById(saved.getId())) {
            Stats s = new Stats();
            s.setDownload(saved);
            s.setAvgSpeedBps(0.0);
            s.setMaxSpeedBps(0.0);
            s.setRetries(0);
            s.setLastStartedAt(null);
            s.setLastFinishedAt(null);
            stats.save(s);
        }

        return saved;
    }

    @Transactional
    public void setStatusForUser(Long userId, UUID downloadId, DownloadStatus status) {
        var d = downloads.findByIdAndOwner_Id(downloadId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Завантаження не знайдено або вам не належить"));

        DownloadStatus prev = d.getStatus();

        if (prev == DownloadStatus.CANCELED && status == DownloadStatus.RUNNING) {
            segments.deleteByDownload_Id(downloadId);
            d.setReceivedBytes(0L);
            d.setTotalBytes(0L);
        }

        d.setStatus(status);
        d.setUpdatedAt(Instant.now());
        downloads.save(d);

        Stats s = stats.findById(downloadId).orElseGet(() -> {
            Stats ns = new Stats();
            ns.setDownload(d);
            ns.setAvgSpeedBps(0.0);
            ns.setMaxSpeedBps(0.0);
            ns.setRetries(0);
            ns.setLastStartedAt(null);
            ns.setLastFinishedAt(null);
            return ns;
        });

        Instant now = Instant.now();

        if (status == DownloadStatus.RUNNING) {
            if (prev != DownloadStatus.RUNNING) {
                s.setRetries(s.getRetries() + 1);
            }
            s.setLastStartedAt(now);
        }

        if (status == DownloadStatus.COMPLETED
                || status == DownloadStatus.CANCELED
                || status == DownloadStatus.ERROR) {
            s.setLastFinishedAt(now);
        }

        stats.save(s);
    }


    @Transactional
    public void deleteForUser(Long userId, UUID downloadId) {
        var d = downloads.findByIdAndOwner_Id(downloadId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Завантаження не знайдено або вам не належить"));

        Path filePath = pathFor(d);

        d.setStatus(DownloadStatus.CANCELED);
        d.setUpdatedAt(Instant.now());
        downloads.save(d);

        List<Future<?>> futures = activeTaskRegistry.drain(downloadId);
        for (Future<?> f : futures) {
            f.cancel(true);
        }

        segments.deleteByDownload_Id(downloadId);
        stats.deleteById(downloadId);
        downloads.deleteById(downloadId);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (Files.deleteIfExists(filePath)) {
                    log.info("[DownloadService] deleted file {}", filePath);
                    return;
                } else {
                    log.info("[DownloadService] file not found or already deleted: {}", filePath);
                    return;
                }
            } catch (Exception e) {
                log.warn("[DownloadService] file {} is busy (attempt {}), will retry: {}",
                        filePath, attempt, e.getMessage());
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("[DownloadService] could not delete file {} after 3 attempts", filePath);
    }


    @Transactional(readOnly = true)
    public SegmentIterator iteratorForDownload(UUID downloadId,
                                               SegmentOrder order,
                                               boolean onlyPending) {
        List<Segment> list = segments.findByDownload_Id(downloadId);
        var collection = new DbSegmentCollection(list, order, onlyPending);
        return collection.iterator();
    }

    @Transactional(readOnly = true)
    public List<Segment> pickSegmentsForRun(UUID downloadId, int limit) {
        var it = iteratorForDownload(downloadId, SegmentOrder.BY_LEFTMOST_GAP, true);
        int cap = Math.max(1, limit);

        List<Segment> picked = new ArrayList<>(cap);
        while (it.hasNext() && picked.size() < cap) {
            picked.add(it.next());
        }
        return picked;
    }


    public String filenameFromUrl(String url) {
        try {
            var u = new URI(url);
            String path = u.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) return "download.bin";
            int slash = path.lastIndexOf('/');
            String name = (slash >= 0) ? path.substring(slash + 1) : path;
            if (name.isBlank()) return "download.bin";
            return name;
        } catch (Exception e) {
            return "download.bin";
        }
    }

    public String defaultSavePath() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) home = ".";
        return Paths.get(home, "Downloads").toString();
    }

    public Path pathFor(Download d) {
        String base = (d.getSavePath() == null || d.getSavePath().isBlank())
                ? defaultSavePath() : d.getSavePath();
        String name = (d.getFileName() == null || d.getFileName().isBlank())
                ? filenameFromUrl(d.getUrl()) : d.getFileName();
        return Paths.get(base).resolve(name);
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
