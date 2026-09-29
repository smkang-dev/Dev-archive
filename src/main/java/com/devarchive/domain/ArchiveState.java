package com.devarchive.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "archive_state")
public class ArchiveState {
  @Id private Long id;

  @Column(nullable = false)
  private long revision;

  @Column(nullable = false)
  private boolean initialized;

  protected ArchiveState() {}

  public long getRevision() {
    return revision;
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void initialize() {
    initialized = true;
    revision++;
  }

  public void advance() {
    revision++;
  }
}
