package org.example.dlm.service;

import lombok.RequiredArgsConstructor;
import org.example.dlm.domain.User;
import org.example.dlm.repo.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo users;
    private final PasswordEncoder encoder;

    @Transactional
    public User register(String username, String rawPassword) {
        users.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Користувач уже існує");
        });
        var u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        return users.save(u);
    }
}
