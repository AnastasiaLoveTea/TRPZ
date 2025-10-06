// repo/DownloadRepo.java
package org.example.dlm.repo;

import org.example.dlm.domain.Download;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DownloadRepo extends JpaRepository<Download, UUID> {
    List<Download> findByOwnerId(Long ownerId);
}

