package org.example.dlm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dlm.domain.Download;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.domain.Segment;
import org.example.dlm.domain.SegmentStatus;
import org.example.dlm.domain.Settings;
import org.example.dlm.repo.DownloadRepo;
import org.example.dlm.repo.SegmentRepo;
import org.example.dlm.repo.StatsRepo;
import org.example.dlm.service.stats.SpeedMeter;
import org.example.dlm.service.stats.StatsObserver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadEngine {

    private final DownloadRepo downloads;
    private final SegmentRepo segments;
    private final StatsRepo stats;
    private final DownloadService downloadService;
    private final SettingsService settingsService;
    private final ActiveTaskRegistry activeTaskRegistry;

    private final SpeedMeter speedMeter;

    @SuppressWarnings("unused")
    private final StatsObserver statsObserver;

    private final @Qualifier("downloadExecutorService") ExecutorService downloadExecutor;

    private final HttpRangeClient http = new HttpRangeClient();

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        List<Download> active = downloads.findAll().stream()
                .filter(d -> d.getStatus() == DownloadStatus.RUNNING)
                .toList();

        if (!active.isEmpty()) {
            log.info("[Engine] tick: {} active downloads", active.size());
        }

        for (Download d : active) {
            try {
                runDownload(d);
            } catch (Exception ex) {
                log.error("[Engine] ERROR in runDownload for {}: {}", d.getId(), ex.toString(), ex);
                d.setStatus(DownloadStatus.ERROR);
                d.setUpdatedAt(Instant.now());
                downloads.save(d);
            }
        }
    }

    private void runDownload(Download d) throws Exception {
        if (d.getFileName() == null || d.getFileName().isBlank()) {
            d.setFileName(downloadService.filenameFromUrl(d.getUrl()));
        }
        if (d.getSavePath() == null || d.getSavePath().isBlank()) {
            d.setSavePath(downloadService.defaultSavePath());
        }
        downloads.save(d);

        Path filePath = downloadService.pathFor(d);
        Files.createDirectories(filePath.getParent());

        Settings settings = settingsService.getOrCreate(d.getOwner().getId());
        int defaultSegments = Math.max(1, settings.getDefaultSegments());
        long globalSpeedLimitBps = Math.max(0, settings.getGlobalSpeedLimitBps());

        List<Segment> segs = segments.findByDownload_Id(d.getId());
        if (segs.isEmpty()) {
            log.info("[Engine] probe {} ({})", d.getId(), d.getUrl());

            var probe = http.probe(URI.create(d.getUrl()));
            d.setUpdatedAt(Instant.now());
            if (probe.contentLength > 0) {
                d.setTotalBytes(probe.contentLength);
            }
            downloads.save(d);

            log.info("[Engine] probe result: rangeSupported={} totalBytes={}",
                    probe.rangeSupported, d.getTotalBytes());

            if (probe.rangeSupported && d.getTotalBytes() > 0) {
                long part = Math.max(1, d.getTotalBytes() / defaultSegments);
                long start = 0;
                for (int i = 0; i < defaultSegments; i++) {
                    long end = (i == defaultSegments - 1)
                            ? d.getTotalBytes() - 1
                            : (start + part - 1);
                    createSegment(d, i, start, end);
                    start = end + 1;
                }
            } else {
                createSegment(d, 0, 0, d.getTotalBytes() > 0 ? d.getTotalBytes() - 1 : -1);
            }
            segs = segments.findByDownload_Id(d.getId());
            log.info("[Engine] created {} segments for {}", segs.size(), d.getId());
        }

        int parallelCap = defaultSegments;

        List<Segment> toStart = downloadService.pickSegmentsForRun(d.getId(), parallelCap);
        log.info("[Engine] picked {} segments to start for {}", toStart.size(), d.getId());

        for (Segment s : toStart) {
            if (s.getStatus() != SegmentStatus.PENDING) continue;

            s.setStatus(SegmentStatus.RUNNING);
            segments.save(s);

            boolean rangeSupported = isRangeSupportedForDownload(d);
            var task = new SegmentTask(
                    new HttpRangeClient(),
                    segments,
                    downloads,
                    d,
                    s,
                    filePath,
                    rangeSupported,
                    globalSpeedLimitBps,
                    defaultSegments,
                    speedMeter
            );
            log.info("[Engine] submit SegmentTask download={} segId={} idx={} (limit={} B/s, segs={})",
                    d.getId(), s.getId(), s.getIdx(), globalSpeedLimitBps, defaultSegments);

            Future<?> future = downloadExecutor.submit(task);
            activeTaskRegistry.register(d.getId(), future);
        }

        boolean left = segments.findByDownload_Id(d.getId()).stream()
                .anyMatch(s -> s.getStatus() != SegmentStatus.DONE);
        if (!left) {
            d.setStatus(DownloadStatus.COMPLETED);
            d.setUpdatedAt(Instant.now());
            downloads.save(d);
            log.info("[Engine] download {} COMPLETED", d.getId());

            stats.findById(d.getId()).ifPresent(st -> {
                st.setLastFinishedAt(Instant.now());
                stats.save(st);
            });
        }
    }

    private void createSegment(Download d, int idx, long start, long end) {
        Segment s = new Segment();
        s.setDownload(d);
        s.setIdx(idx);
        s.setStartByte(start);
        s.setEndByte(end);
        s.setReceivedBytes(0);
        s.setStatus(SegmentStatus.PENDING);
        segments.save(s);
        log.info("[Engine] segment created download={} segId={} idx={} [{}, {}]",
                d.getId(), s.getId(), idx, start, end);
    }

    private boolean isRangeSupportedForDownload(Download d) {
        var segs = segments.findByDownload_Id(d.getId());
        return !segs.isEmpty()
                && segs.stream().allMatch(s -> s.getEndByte() >= s.getStartByte());
    }

}
