package org.example.dlm.service.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpeedSample {
    private final long bytes;
    private final long timestampMs;
}
