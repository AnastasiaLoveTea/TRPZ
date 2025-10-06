package org.example.dlm.repo;

import org.example.dlm.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepo extends JpaRepository<Settings, Long> {}

