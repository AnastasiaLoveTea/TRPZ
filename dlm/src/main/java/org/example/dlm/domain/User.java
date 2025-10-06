package org.example.dlm.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity @Table(name="users")
@lombok.Getter @lombok.Setter @lombok.NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false, name="password_hash")
    private String passwordHash;
}

