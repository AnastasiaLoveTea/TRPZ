package org.example.dlm.service.stats;

import lombok.extern.slf4j.Slf4j;
import org.example.dlm.observer.Observer;
import org.example.dlm.observer.Subject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SpeedMeter implements Subject {

    private static final long WINDOW_MS = 3000L;

    private final Map<UUID, List<SpeedSample>> samplesByDownload = new ConcurrentHashMap<>();

    private final Map<UUID, SpeedState> stateByDownload = new ConcurrentHashMap<>();

    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    @Override
    public void attach(Observer observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer o : observers) {
            try {
                o.update(this);
            } catch (Exception ex) {
                log.warn("[SpeedMeter] observer {} threw exception: {}",
                        o.getClass().getSimpleName(), ex.toString(), ex);
            }
        }
    }

    public void onBytesDownloaded(UUID downloadId, long bytes) {
        onBytesDownloaded(downloadId, bytes, System.currentTimeMillis());
    }

    public void onBytesDownloaded(UUID downloadId, long bytes, long timestampMs) {
        if (downloadId == null || bytes <= 0) return;

        List<SpeedSample> list = samplesByDownload
                .computeIfAbsent(downloadId, id -> new CopyOnWriteArrayList<>());

        list.add(new SpeedSample(bytes, timestampMs));
    }

    public Optional<SpeedState> getState(UUID downloadId) {
        if (downloadId == null) return Optional.empty();
        return Optional.ofNullable(stateByDownload.get(downloadId));
    }

    public Map<UUID, SpeedState> getAllStatesSnapshot() {
        return stateByDownload.entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    @Scheduled(fixedDelay = 1000)
    public void recalcAndNotify() {
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, List<SpeedSample>> entry : samplesByDownload.entrySet()) {
            UUID downloadId = entry.getKey();
            List<SpeedSample> rawList = entry.getValue();

            List<SpeedSample> samples = new ArrayList<>(rawList);

            long cutoff = now - WINDOW_MS;
            samples.removeIf(s -> s.getTimestampMs() < cutoff);

            rawList.removeIf(s -> s.getTimestampMs() < cutoff);

            SpeedState state = computeState(samples);
            stateByDownload.put(downloadId, state);
        }

        notifyObservers();
    }

    private SpeedState computeState(List<SpeedSample> samples) {
        if (samples.isEmpty()) {
            return new SpeedState(0.0, 0.0, WINDOW_MS);
        }

        samples.sort(Comparator.comparingLong(SpeedSample::getTimestampMs));

        long totalBytes = 0L;
        long minTs = samples.get(0).getTimestampMs();
        long maxTs = minTs;

        for (SpeedSample s : samples) {
            totalBytes += s.getBytes();
            long ts = s.getTimestampMs();
            if (ts > maxTs) maxTs = ts;
        }

        long durationMs = maxTs - minTs;
        if (durationMs <= 0) durationMs = 1;

        double avgSpeedBps = totalBytes * 1000.0 / durationMs;

        double windowBasedMax = avgSpeedBps;

        return new SpeedState(avgSpeedBps, windowBasedMax, WINDOW_MS);
    }
}
