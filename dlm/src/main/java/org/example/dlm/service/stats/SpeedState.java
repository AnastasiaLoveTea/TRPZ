package org.example.dlm.service.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpeedState {

    private final double avgSpeedBps;

    private final double maxSpeedBps;

    private final long windowMillis;
}
