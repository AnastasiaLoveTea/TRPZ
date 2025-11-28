package org.example.dlm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dlm.domain.Download;
import org.example.dlm.domain.DownloadStatus;
import org.example.dlm.domain.Segment;
import org.example.dlm.domain.SegmentStatus;
import org.example.dlm.repo.DownloadRepo;
import org.example.dlm.repo.SegmentRepo;
import org.example.dlm.service.stats.SpeedMeter;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class SegmentTask implements Runnable {

    private final HttpRangeClient http;

    private final SegmentRepo segments;
    private final DownloadRepo downloads;

    private final Download download;
    private final Segment segment;
    private final Path filePath;
    private final boolean rangeSupported;

    private final long globalSpeedLimitBps;
    private final int segmentsPerDownload;

    private final SpeedMeter speedMeter;

    private long windowStartMs = 0L;
    private long bytesInWindow = 0L;

    @Override
    public void run() {
        try {
            Optional<Segment> segOpt = segments.findById(segment.getId());
            if (segOpt.isEmpty()) {
                log.warn("[SegmentTask] segment {} not found in DB, abort", segment.getId());
                return;
            }
            Segment currentSeg = segOpt.get();

            Optional<Download> dOpt = downloads.findById(download.getId());
            if (dOpt.isEmpty()) {
                log.warn("[SegmentTask] download {} not found in DB, abort", download.getId());
                return;
            }
            if (dOpt.get().getStatus() != DownloadStatus.RUNNING) {
                log.info("[SegmentTask] download {} is {}, skip segment {}",
                        download.getId(), dOpt.get().getStatus(), segment.getId());
                return;
            }

            long startByte = currentSeg.getStartByte();
            long endByte = currentSeg.getEndByte();

            long baseReceived = Math.max(0, currentSeg.getReceivedBytes());
            long segLength = (endByte >= startByte && endByte >= 0)
                    ? (endByte - startByte + 1)
                    : -1;

            log.info("[SegmentTask] START download={} segId={} idx={} start={} end={} baseReceived={} len={} rangeSupported={} limit={}B/s segs={}",
                    download.getId(), currentSeg.getId(), currentSeg.getIdx(),
                    startByte, endByte, baseReceived, segLength, rangeSupported,
                    globalSpeedLimitBps, segmentsPerDownload);

            if (segLength > 0 && baseReceived >= segLength) {
                log.info("[SegmentTask] segment {} already fully downloaded ({} of {}), mark DONE",
                        currentSeg.getId(), baseReceived, segLength);
                currentSeg.setStatus(SegmentStatus.DONE);
                segments.save(currentSeg);
                recalcDownloadReceived();
                return;
            }

            currentSeg.setStatus(SegmentStatus.RUNNING);
            segments.save(currentSeg);

            long effectiveStart = startByte + baseReceived;

            URI uri = URI.create(download.getUrl());
            String rangeHeader;
            long seekTo;

            if (rangeSupported) {
                if (endByte >= 0 && endByte >= effectiveStart) {
                    rangeHeader = "bytes=" + effectiveStart + "-" + endByte;
                } else {
                    rangeHeader = "bytes=" + effectiveStart + "-";
                }
                seekTo = effectiveStart;
            } else {
                if (effectiveStart > 0) {
                    log.warn("[SegmentTask] server has no Range, but effectiveStart={} > 0; restarting from segment start", effectiveStart);
                    baseReceived = 0;
                    effectiveStart = startByte;
                }
                rangeHeader = null;
                seekTo = effectiveStart;
            }

            log.info("[SegmentTask] HTTP request download={} segId={} rangeHeader='{}' seekTo={}",
                    download.getId(), currentSeg.getId(), rangeHeader, seekTo);

            var req = HttpRangeClient.buildRequest(uri, rangeHeader);
            var resp = HttpRangeClient.CLIENT.send(req, HttpRangeClient.STREAM_HANDLER);

            int code = resp.statusCode();
            long contentLength = resp.headers()
                    .firstValue("Content-Length")
                    .map(s -> {
                        try {
                            return Long.parseLong(s.trim());
                        } catch (Exception e) {
                            return -1L;
                        }
                    }).orElse(-1L);

            log.info("[SegmentTask] HTTP response download={} segId={} status={} contentLength={}",
                    download.getId(), currentSeg.getId(), code, contentLength);

            try (InputStream in = resp.body();
                 RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {

                if (seekTo > 0) {
                    raf.seek(seekTo);
                }

                byte[] buf = new byte[64 * 1024];
                int read;

                long lastUpdate = System.currentTimeMillis();
                long lastStatusCheck = lastUpdate;
                long lastLog = lastUpdate;
                long receivedThisRun = 0L;

                windowStartMs = System.currentTimeMillis();
                bytesInWindow = 0L;

                while ((read = in.read(buf)) != -1) {

                    if (Thread.currentThread().isInterrupted()) {
                        log.info("[SegmentTask] interrupted download={} segId={}, treat as CANCEL",
                                download.getId(), segment.getId());
                        handleCancel(baseReceived, receivedThisRun);
                        return;
                    }

                    long now = System.currentTimeMillis();

                    if (now - lastStatusCheck >= 500) {
                        Optional<Download> curDownloadOpt = downloads.findById(download.getId());
                        if (curDownloadOpt.isPresent()) {
                            Download curDownload = curDownloadOpt.get();
                            DownloadStatus st = curDownload.getStatus();
                            if (st == DownloadStatus.PAUSED) {
                                log.info("[SegmentTask] PAUSE requested for download={}, segId={}",
                                        download.getId(), currentSeg.getId());
                                handlePause(baseReceived, receivedThisRun);
                                return;
                            } else if (st == DownloadStatus.CANCELED) {
                                log.info("[SegmentTask] CANCEL requested for download={}, segId={}",
                                        download.getId(), currentSeg.getId());
                                handleCancel(baseReceived, receivedThisRun);
                                return;
                            }
                        } else {
                            log.info("[SegmentTask] download {} already deleted, stop segId={}",
                                    download.getId(), currentSeg.getId());
                            return;
                        }
                        lastStatusCheck = now;
                    }

                    raf.write(buf, 0, read);
                    receivedThisRun += read;

                    if (read > 0 && speedMeter != null) {
                        speedMeter.onBytesDownloaded(download.getId(), read);
                    }

                    if (now - lastUpdate >= 300) {
                        updateSegmentAndDownloadProgress(baseReceived, receivedThisRun);
                        lastUpdate = now;
                    }

                    if (globalSpeedLimitBps > 0) {
                        applySpeedLimit(read);
                    }

                    if (now - lastLog >= 2000) {
                        log.info("[SegmentTask] streaming download={} segId={} totalForSegment={} (base={} + run={})",
                                download.getId(), currentSeg.getId(),
                                baseReceived + receivedThisRun, baseReceived, receivedThisRun);
                        lastLog = now;
                    }
                }

                updateSegmentAndDownloadProgress(baseReceived, receivedThisRun);
                log.info("[SegmentTask] FINISH read stream download={} segId={} totalForSegment={}",
                        download.getId(), currentSeg.getId(), baseReceived + receivedThisRun);
            }

            downloads.findById(download.getId()).ifPresent(d -> {
                if (d.getStatus() == DownloadStatus.RUNNING) {
                    segments.findById(segment.getId()).ifPresent(s -> {
                        s.setStatus(SegmentStatus.DONE);
                        segments.save(s);
                        log.info("[SegmentTask] segment DONE download={} segId={}",
                                download.getId(), s.getId());
                    });
                    recalcDownloadReceived();
                } else {
                    log.info("[SegmentTask] download {} is {} after stream, not marking DONE for segment {}",
                            d.getId(), d.getStatus(), segment.getId());
                }
            });

        } catch (Exception ex) {
            handleError(ex);
        }
    }

    private void applySpeedLimit(int justReadBytes) {
        if (globalSpeedLimitBps <= 0) return;

        long perSegmentLimit = Math.max(1L,
                globalSpeedLimitBps / Math.max(1, segmentsPerDownload));

        long now = System.currentTimeMillis();
        if (windowStartMs == 0L) {
            windowStartMs = now;
        }

        bytesInWindow += justReadBytes;
        long elapsed = now - windowStartMs;
        if (elapsed <= 0) {
            return;
        }

        long allowed = perSegmentLimit * elapsed / 1000L;

        if (bytesInWindow > allowed) {
            long extra = bytesInWindow - allowed;
            long sleepMs = (extra * 1000L) / perSegmentLimit;
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    handleCancel(0, 0); // прогрес уже збережений вище
                }
            }
        }

        if (elapsed >= 1000L) {
            windowStartMs = now;
            bytesInWindow = 0L;
        }
    }

    private void updateSegmentAndDownloadProgress(long baseReceived, long receivedThisRun) {
        long totalForSegment = Math.max(0, baseReceived + receivedThisRun);

        segments.findById(segment.getId()).ifPresent(s -> {
            s.setReceivedBytes(totalForSegment);
            s.setStatus(SegmentStatus.RUNNING);
            segments.save(s);
        });

        recalcDownloadReceived();
    }

    private void recalcDownloadReceived() {
        try {
            downloads.findById(download.getId()).ifPresent(d -> {
                long sum = segments.findByDownload_Id(d.getId()).stream()
                        .mapToLong(Segment::getReceivedBytes)
                        .sum();
                d.setReceivedBytes(sum);
                d.setUpdatedAt(Instant.now());
                downloads.save(d);
            });
        } catch (Exception ex) {
            log.warn("[SegmentTask] recalc skipped, download may be deleted or locked: {}", ex.toString());
        }
    }

    private void handlePause(long baseReceived, long receivedThisRun) {
        long totalForSegment = Math.max(0, baseReceived + receivedThisRun);

        segments.findById(segment.getId()).ifPresent(s -> {
            s.setReceivedBytes(totalForSegment);
            s.setStatus(SegmentStatus.PENDING);
            segments.save(s);
        });

        recalcDownloadReceived();
    }

    private void handleCancel(long baseReceived, long receivedThisRun) {
        long totalForSegment = Math.max(0, baseReceived + receivedThisRun);

        segments.findById(segment.getId()).ifPresent(s -> {
            s.setReceivedBytes(totalForSegment);
            s.setStatus(SegmentStatus.CANCELED);
            segments.save(s);
        });

        recalcDownloadReceived();
    }

    private void handleError(Exception ex) {
        log.error("[SegmentTask] ERROR download={} segment={} : {}",
                download.getId(), segment.getId(), ex.toString(), ex);

        segments.findById(segment.getId()).ifPresent(s -> {
            s.setStatus(SegmentStatus.ERROR);
            segments.save(s);
        });

        try {
            downloads.findById(download.getId()).ifPresent(d -> {
                if (d.getStatus() == DownloadStatus.RUNNING) {
                    d.setStatus(DownloadStatus.ERROR);
                    d.setUpdatedAt(Instant.now());
                    downloads.save(d);
                }
            });
        } catch (Exception e) {
            log.warn("[SegmentTask] cannot update download status after error: {}", e.toString());
        }
    }
}
