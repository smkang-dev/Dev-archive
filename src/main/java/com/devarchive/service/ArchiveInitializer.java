package com.devarchive.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ArchiveInitializer implements ApplicationRunner {
  private final CategoryService service;

  public ArchiveInitializer(CategoryService service) {
    this.service = service;
  }

  @Override
  public void run(ApplicationArguments args) {
    service.initializeData();
  }
}
