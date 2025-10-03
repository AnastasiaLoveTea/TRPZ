package org.example.dlm.repo;

import org.example.dlm.domain.Download;
import org.springframework.data.repository.CrudRepository;
import java.util.*;

public interface DownloadRepo extends CrudRepository<Download, UUID> {
    List<Download> findByOwnerId(Long ownerId);
}
