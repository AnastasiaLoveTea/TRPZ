package org.example.dlm.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="downloads")
@lombok.Getter @lombok.Setter @lombok.NoArgsConstructor
public class Download {
    @Id
    private UUID id;

    @Column(length = 2048, nullable = false)
    private String url;

    private String fileName;
    private String savePath;

    private long totalBytes;
    private long receivedBytes;

    @Enumerated(EnumType.STRING)
    private DownloadStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="owner_id", nullable=false)
    private User owner;
}

