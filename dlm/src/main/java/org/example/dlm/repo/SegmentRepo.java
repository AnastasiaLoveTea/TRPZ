package org.example.dlm.repo;

import org.example.dlm.domain.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SegmentRepo extends JpaRepository<Segment, Long> {
    List<Segment> findByDownload_Id(UUID downloadId);
    void deleteByDownload_Id(UUID downloadId);
}

