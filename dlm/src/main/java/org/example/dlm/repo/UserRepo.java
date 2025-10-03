package org.example.dlm.repo;

import org.example.dlm.domain.User;
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

public interface UserRepo extends CrudRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

