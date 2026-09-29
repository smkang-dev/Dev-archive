package com.devarchive.service;

import com.devarchive.domain.ArchiveState;
import com.devarchive.repository.ArchiveStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class WriteGuard {
  private final ArchiveStateRepository repository;

  public WriteGuard(ArchiveStateRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public ArchiveState lock() {
    return repository
        .lockState()
        .orElseThrow(() -> new IllegalStateException("archive_state 초기화 SQL을 먼저 실행해 주세요."));
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void begin(Long expectedRevision) {
    if (expectedRevision == null || expectedRevision < 0)
      throw new IllegalArgumentException("화면 버전이 없습니다. 최신 화면을 열어 주세요.");
    ArchiveState state = lock();
    if (!state.isInitialized()) throw new IllegalStateException("초기화가 완료되지 않았습니다.");
    if (state.getRevision() != expectedRevision) throw new StaleRevisionException();
    state.advance();
  }
}
