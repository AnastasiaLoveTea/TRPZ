package org.example.dlm.service.stats;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dlm.domain.Download;
import org.example.dlm.domain.Stats;
import org.example.dlm.observer.Observer;
import org.example.dlm.observer.Subject;
import org.example.dlm.repo.DownloadRepo;
import org.example.dlm.repo.StatsRepo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsObserver implements Observer {

    private final StatsRepo statsRepo;
    private final DownloadRepo downloadRepo;

    private final SpeedMeter speedMeter;

    @PostConstruct
    public void register() {
        speedMeter.attach(this);
        log.info("[StatsObserver] registered as observer for SpeedMeter");
    }

    @Override
    public void update(Subject subject) {
        if (!(subject instanceof SpeedMeter meter)) {
            return;
        }

        Map<UUID, SpeedState> states = meter.getAllStatesSnapshot();
        if (states.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, SpeedState> entry : states.entrySet()) {
            UUID downloadId = entry.getKey();
            SpeedState state = entry.getValue();

            if (state == null) continue;

            downloadRepo.findById(downloadId).ifPresent(download -> {
                Stats stats = statsRepo.findById(downloadId)
                        .orElseGet(() -> createStats(download));

                double avgSpeed = state.getAvgSpeedBps();
                double windowMax = state.getMaxSpeedBps();

                stats.setAvgSpeedBps(avgSpeed);

                double prevMax = stats.getMaxSpeedBps();
                double newMax = Math.max(prevMax, windowMax);
                stats.setMaxSpeedBps(newMax);

                statsRepo.save(stats);
            });
        }
    }

    private Stats createStats(Download download) {
        Stats stats = new Stats();
        stats.setDownload(download);
        stats.setAvgSpeedBps(0.0);
        stats.setMaxSpeedBps(0.0);
        return stats;
    }
}
