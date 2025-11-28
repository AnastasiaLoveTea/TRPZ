package org.example.dlm.repo;

import org.example.dlm.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingsRepo extends JpaRepository<Settings, Long> {
    Optional<Settings> findByUser_Id(Long userId);
}
