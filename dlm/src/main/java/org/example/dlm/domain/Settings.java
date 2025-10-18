package org.example.dlm.domain;

import jakarta.persistence.*;

@Entity @Table(name="settings",
        uniqueConstraints=@UniqueConstraint(columnNames={"user_id"}))
@lombok.Getter @lombok.Setter @lombok.NoArgsConstructor
public class Settings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name="user_id")
    private User user;

    private int  defaultSegments = 1;
    private long globalSpeedLimitBps = 0;
}
