package org.example.dlm.repo;

import org.example.dlm.domain.Settings;
import org.springframework.data.repository.CrudRepository;

public interface SettingsRepo extends CrudRepository<Settings, Long> {}

