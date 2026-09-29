package com.devarchive.service;

import com.devarchive.controller.LayoutOrderRequest;
import com.devarchive.domain.*;
import com.devarchive.repository.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LayoutService {
  public static final int MAX_EDIT_ARCHIVES = 500;
  private final CategoryRepository categories;
  private final ArchiveRepository archives;
  private final WriteGuard guard;

  public LayoutService(
      CategoryRepository categories, ArchiveRepository archives, WriteGuard guard) {
    this.categories = categories;
    this.archives = archives;
    this.guard = guard;
  }

  @Transactional
  public void reorder(LayoutOrderRequest request) {
    if (request == null) throw invalid();
    guard.begin(request.expectedRevision());
    if (archives.count() > MAX_EDIT_ARCHIVES)
      throw new IllegalArgumentException("순서 편집은 자료 500개 이하에서 지원합니다.");
    Map<Long, Category> cats =
        categories.findAll().stream()
            .collect(Collectors.toMap(Category::getId, Function.identity()));
    Map<Long, Archive> items =
        archives.findAll().stream().collect(Collectors.toMap(Archive::getId, Function.identity()));
    List<Long> ids = request.categoryIds();
    if (ids == null
        || ids.stream().anyMatch(Objects::isNull)
        || ids.size() != cats.size()
        || !new HashSet<>(ids).equals(cats.keySet())) throw invalid();
    for (int i = 0; i < ids.size(); i++)
      if (cats.get(ids.get(i)).isDefaultCategory() && i != ids.size() - 1) throw invalid();
    var groups = request.archiveGroups();
    if (groups == null || groups.size() != cats.size()) throw invalid();
    Set<Long> seenCategories = new HashSet<>(), seenArchives = new HashSet<>();
    for (var group : groups) {
      if (group == null
          || !cats.containsKey(group.categoryId())
          || !seenCategories.add(group.categoryId())
          || group.archiveIds() == null) throw invalid();
      for (Long id : group.archiveIds())
        if (id == null || !items.containsKey(id) || !seenArchives.add(id)) throw invalid();
    }
    if (!seenCategories.equals(cats.keySet()) || !seenArchives.equals(items.keySet()))
      throw invalid();
    // Mutate only after the entire snapshot has been validated.
    for (int i = 0; i < ids.size(); i++)
      if (!cats.get(ids.get(i)).isDefaultCategory()) cats.get(ids.get(i)).changeSortOrder(i);
    for (var group : groups)
      for (int i = 0; i < group.archiveIds().size(); i++) {
        Archive item = items.get(group.archiveIds().get(i));
        item.moveTo(cats.get(group.categoryId()));
        item.changeSortOrder(i);
      }
  }

  private IllegalArgumentException invalid() {
    return new IllegalArgumentException("순서 정보에 중복·누락 또는 잘못된 ID가 있습니다. 최신 화면을 열어 주세요.");
  }
}
