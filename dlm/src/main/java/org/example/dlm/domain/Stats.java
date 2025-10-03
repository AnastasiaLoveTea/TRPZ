package org.example.dlm.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="stats")
@lombok.Getter @lombok.Setter @lombok.NoArgsConstructor
public class Stats {
    @Id
    @Column(name="download_id")
    private UUID downloadId;

    @OneToOne @MapsId @JoinColumn(name="download_id")
    private Download download;

    private double avgSpeedBps;
    private double maxSpeedBps;
    private int retries;
    private Instant lastStartedAt;
    private Instant lastFinishedAt;
}
