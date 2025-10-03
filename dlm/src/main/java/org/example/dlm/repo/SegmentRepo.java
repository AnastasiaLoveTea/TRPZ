package org.example.dlm.repo;

import org.example.dlm.domain.Segment;
import org.springframework.data.repository.CrudRepository;
import java.util.*;
import java.util.UUID;

public interface SegmentRepo extends CrudRepository<Segment, Long> {
    List<Segment> findByDownloadId(UUID downloadId);
    void deleteByDownloadId(UUID downloadId);
}

