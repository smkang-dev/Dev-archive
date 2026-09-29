package com.devarchive.service;

import com.devarchive.domain.*;
import com.devarchive.repository.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ArchiveViewService {
  private final ArchiveRepository archives;
  private final CategoryRepository categories;
  private final ArchiveStateRepository states;

  public ArchiveViewService(
      ArchiveRepository archives, CategoryRepository categories, ArchiveStateRepository states) {
    this.archives = archives;
    this.categories = categories;
    this.states = states;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Map<String, Object> load(Map<String, String> params) {
    long revision = states.findById(1L).orElseThrow().getRevision();
    String keyword = params.getOrDefault("keyword", "").trim();
    if (keyword.length() > 200) throw new IllegalArgumentException("검색어는 200자 이하로 입력해 주세요.");
    boolean searching = !keyword.isEmpty(),
        editing = "true".equals(params.get("edit")) && !searching;
    var cats = categories.findAllInDisplayOrder();
    Map<Long, List<Archive>> byCategory = new HashMap<>();
    Map<Long, Integer> currentPages = new HashMap<>(), totalPages = new HashMap<>();
    long total = 0;
    if (editing) {
      if (archives.count() > LayoutService.MAX_EDIT_ARCHIVES)
        throw new IllegalArgumentException("순서 편집은 자료 500개 이하에서 지원합니다.");
      for (Archive a : archives.findAllInDisplayOrder())
        byCategory.computeIfAbsent(a.getCategory().getId(), k -> new ArrayList<>()).add(a);
      for (Category c : cats) {
        currentPages.put(c.getId(), 1);
        totalPages.put(c.getId(), 1);
      }
    } else {
      String pattern =
          "%"
              + keyword
                  .toLowerCase(Locale.ROOT)
                  .replace("!", "!!")
                  .replace("%", "!%")
                  .replace("_", "!_")
              + "%";
      for (Category c : cats) {
        int requested = page(params.get("p" + c.getId()));
        var result = archives.pageByCategory(c.getId(), pattern, PageRequest.of(requested - 1, 4));
        int pages = Math.max(1, result.getTotalPages());
        if (requested > pages) {
          requested = pages;
          result = archives.pageByCategory(c.getId(), pattern, PageRequest.of(requested - 1, 4));
        }
        byCategory.put(c.getId(), result.getContent());
        currentPages.put(c.getId(), requested);
        totalPages.put(c.getId(), pages);
        total += result.getTotalElements();
      }
    }
    Map<String, Object> model = new HashMap<>();
    model.put("revision", revision);
    model.put("categories", cats);
    model.put("archivesByCategory", byCategory);
    model.put("currentPages", currentPages);
    model.put("totalPages", totalPages);
    model.put("keyword", keyword);
    model.put("searching", searching);
    model.put("editing", editing);
    model.put("searchResultCount", total);
    return model;
  }

  private int page(String value) {
    try {
      return Math.max(1, Math.min(100000, Integer.parseInt(value)));
    } catch (Exception ignored) {
      return 1;
    }
  }
}
