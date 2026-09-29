package com.devarchive.repository;

import com.devarchive.domain.ArchiveState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface ArchiveStateRepository extends JpaRepository<ArchiveState, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ArchiveState s where s.id = 1")
  Optional<ArchiveState> lockState();
}
