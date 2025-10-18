package org.example.dlm.domain;

import jakarta.persistence.*;

@Entity @Table(name="segments",
        uniqueConstraints=@UniqueConstraint(columnNames={"download_id","idx"}))
@lombok.Getter @lombok.Setter @lombok.NoArgsConstructor
public class Segment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="download_id", nullable=false)
    private Download download;

    private int idx;
    private long startByte;
    private long endByte;
    private long receivedBytes;

    @Enumerated(EnumType.STRING)
    private SegmentStatus status;
}

