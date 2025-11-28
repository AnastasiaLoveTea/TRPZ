package org.example.dlm.service;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.Settings;
import org.example.dlm.domain.User;
import org.example.dlm.repo.SettingsRepo;
import org.example.dlm.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepo settingsRepo;
    private final UserRepo users;

    @Transactional
    public Settings getOrCreate(Long userId) {
        return settingsRepo.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = users.findById(userId).orElseThrow();
                    Settings s = new Settings();
                    s.setUser(user);
                    s.setDefaultSegments(1);
                    s.setGlobalSpeedLimitBps(0);
                    return settingsRepo.save(s);
                });
    }

    @Transactional
    public Settings update(Long userId, int defaultSegments, long globalSpeedLimitBps) {
        if (defaultSegments < 1) defaultSegments = 1;
        if (globalSpeedLimitBps < 0) globalSpeedLimitBps = 0;

        Settings current = getOrCreate(userId);
        current.setDefaultSegments(defaultSegments);
        current.setGlobalSpeedLimitBps(globalSpeedLimitBps);
        return settingsRepo.save(current);
    }
}
