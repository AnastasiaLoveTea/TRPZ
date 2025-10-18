package org.example.dlm.repo;

import org.example.dlm.domain.Download;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DownloadRepo extends JpaRepository<Download, UUID> {
    List<Download> findByOwnerId(Long ownerId);
    Optional<Download> findByIdAndOwner_Id(UUID id, Long ownerId);
}
