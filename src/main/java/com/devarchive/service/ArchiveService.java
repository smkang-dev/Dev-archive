package com.devarchive.service;

import com.devarchive.domain.Archive;
import com.devarchive.domain.Category;
import com.devarchive.repository.ArchiveRepository;
import com.devarchive.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArchiveService {

  private final ArchiveRepository archiveRepository;
  private final CategoryRepository categoryRepository;

  private final WriteGuard writeGuard;

  public ArchiveService(
      ArchiveRepository archiveRepository,
      CategoryRepository categoryRepository,
      WriteGuard writeGuard) {
    this.archiveRepository = archiveRepository;
    this.categoryRepository = categoryRepository;
    this.writeGuard = writeGuard;
  }

  @Transactional
  public void create(
      Long categoryId, String title, String url, String memo, Long expectedRevision) {
    writeGuard.begin(expectedRevision);
    Category category = findCategory(categoryId);

    int nextOrder = archiveRepository.findMaxSortOrderByCategoryId(categoryId) + 1;

    archiveRepository.save(
        new Archive(
            validateTitle(title), validateUrl(url), normalizeMemo(memo), category, nextOrder));
  }

  @Transactional
  public void update(
      Long archiveId,
      Long categoryId,
      String title,
      String url,
      String memo,
      Long expectedRevision) {
    writeGuard.begin(expectedRevision);
    Archive archive =
        archiveRepository
            .findById(archiveId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자료입니다."));

    Long previousCategoryId = archive.getCategory().getId();

    Category category = findCategory(categoryId);

    boolean categoryChanged = !previousCategoryId.equals(categoryId);

    int targetOrder =
        categoryChanged ? archiveRepository.findMaxSortOrderByCategoryId(categoryId) + 1 : 0;

    archive.update(validateTitle(title), validateUrl(url), normalizeMemo(memo), category);

    if (categoryChanged) {
      archive.changeSortOrder(targetOrder);
      normalizeArchiveOrders(previousCategoryId);
    }
  }

  @Transactional
  public void delete(Long archiveId, Long expectedRevision) {
    writeGuard.begin(expectedRevision);
    Archive archive =
        archiveRepository
            .findById(archiveId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자료입니다."));

    Long categoryId = archive.getCategory().getId();

    archiveRepository.delete(archive);
    archiveRepository.flush();

    normalizeArchiveOrders(categoryId);
  }

  private void normalizeArchiveOrders(Long categoryId) {
    List<Archive> archives = archiveRepository.findByCategoryIdInDisplayOrder(categoryId);

    for (int i = 0; i < archives.size(); i++) {
      archives.get(i).changeSortOrder(i);
    }
  }

  private Category findCategory(Long categoryId) {
    return categoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
  }

  private String validateTitle(String title) {
    String value = title == null ? "" : title.trim();

    if (value.isBlank()) {
      throw new IllegalArgumentException("제목을 입력해 주세요.");
    }

    if (value.length() > 100) {
      throw new IllegalArgumentException("제목은 100자 이하로 입력해 주세요.");
    }

    return value;
  }

  private String validateUrl(String url) {
    String value = url == null ? "" : url.trim();

    if (value.isBlank()) {
      throw new IllegalArgumentException("URL을 입력해 주세요.");
    }

    if (!value.startsWith("http://") && !value.startsWith("https://")) {
      throw new IllegalArgumentException("URL은 http:// 또는 https://로 시작해야 합니다.");
    }

    if (value.length() > 1000) {
      throw new IllegalArgumentException("URL이 너무 깁니다.");
    }

    try {
      java.net.URI uri = new java.net.URI(value);
      if (uri.getHost() == null || uri.getUserInfo() != null)
        throw new IllegalArgumentException("올바른 웹 주소를 입력해 주세요.");
    } catch (java.net.URISyntaxException e) {
      throw new IllegalArgumentException("올바른 웹 주소를 입력해 주세요.");
    }
    return value;
  }

  private String normalizeMemo(String memo) {
    String value = memo == null ? "" : memo.trim();
    if (value.length() > 10000) throw new IllegalArgumentException("메모는 10000자 이하로 입력해 주세요.");
    return value;
  }
}
