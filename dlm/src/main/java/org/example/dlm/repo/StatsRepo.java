package org.example.dlm.repo;

import org.example.dlm.domain.Stats;
import org.springframework.data.repository.CrudRepository;
import java.util.UUID;

public interface StatsRepo extends CrudRepository<Stats, UUID> {}
