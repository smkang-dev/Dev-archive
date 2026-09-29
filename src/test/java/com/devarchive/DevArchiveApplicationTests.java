package com.devarchive;

import static org.junit.jupiter.api.Assertions.*;

import com.devarchive.controller.LayoutOrderRequest;
import com.devarchive.controller.LayoutOrderRequest.ArchiveGroupOrder;
import com.devarchive.domain.*;
import com.devarchive.repository.*;
import com.devarchive.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class DevArchiveApplicationTests {
  @Autowired CategoryRepository categories;
  @Autowired ArchiveRepository archives;
  @Autowired ArchiveStateRepository states;
  @Autowired CategoryService categoryService;
  @Autowired ArchiveService archiveService;
  @Autowired LayoutService layoutService;
  @Autowired ArchiveViewService views;
  @Autowired PlatformTransactionManager manager;
  @Autowired org.springframework.web.context.WebApplicationContext web;
  Long categoryId, otherId, defaultId;

  @BeforeEach
  void prepare() {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            s -> {
              archives.deleteAllInBatch();
              for (Category c : categories.findAll())
                if (!c.isDefaultCategory()) categories.delete(c);
              categories.flush();
            });
    categoryService.create("Java", revision());
    categoryService.create("DB", revision());
    categoryId = categories.findByName("Java").orElseThrow().getId();
    otherId = categories.findByName("DB").orElseThrow().getId();
    defaultId = categories.findByName("기타").orElseThrow().getId();
  }

  long revision() {
    return states.findById(1L).orElseThrow().getRevision();
  }

  void add(String title) {
    archiveService.create(categoryId, title, "https://example.com", "memo", revision());
  }

  LayoutOrderRequest request(List<Long> ids, List<ArchiveGroupOrder> groups) {
    return new LayoutOrderRequest(revision(), ids, groups);
  }

  List<Long> categoryIds() {
    return List.of(categoryId, otherId, defaultId);
  }

  List<ArchiveGroupOrder> groups(List<Long> first) {
    return List.of(
        new ArchiveGroupOrder(categoryId, first),
        new ArchiveGroupOrder(otherId, List.of()),
        new ArchiveGroupOrder(defaultId, List.of()));
  }

  @Test
  void templatesRenderInBrowseSearchAndEditModes() throws Exception {
    add("render title");
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
    for (String url : List.of("/", "/?keyword=render", "/?edit=true")) {
      var result =
          mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url))
              .andReturn();
      assertEquals(200, result.getResponse().getStatus());
      assertTrue(result.getResponse().getContentAsString().contains("render title"));
      assertTrue(result.getResponse().getContentAsString().contains("expectedRevision"));
    }
  }

  @Test
  void archiveUpdateMovesToNextOrderWithoutGap() {
    add("source");
    Long id = archives.findByCategoryIdInDisplayOrder(categoryId).get(0).getId();
    archiveService.create(otherId, "target", "https://example.com", "", revision());
    archiveService.update(
        id, otherId, "edited", "https://example.com/edited", "edited memo", revision());
    assertTrue(archives.findByCategoryIdInDisplayOrder(categoryId).isEmpty());
    var target = archives.findByCategoryIdInDisplayOrder(otherId);
    assertEquals(2, target.size());
    assertEquals(1, target.get(1).getSortOrder());
    assertEquals("edited", target.get(1).getTitle());
  }

  @Test
  void controllerReturnsConflictAndBadRequest() throws Exception {
    var mvc =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(web).build();
    long old = revision();
    add("new");
    var stale =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/categories")
                    .param("name", "stale")
                    .param("expectedRevision", Long.toString(old)))
            .andReturn();
    assertEquals(409, stale.getResponse().getStatus());
    var invalid =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/layout/reorder")
                    .contentType("application/json")
                    .content(
                        "{\"expectedRevision\":"
                            + revision()
                            + ",\"categoryIds\":[],\"archiveGroups\":[]}"))
            .andReturn();
    assertEquals(400, invalid.getResponse().getStatus());
    assertTrue(invalid.getResponse().getContentAsString().contains("message"));
  }

  @Test
  void startupUsesTransactionAndDoesNotRewriteOnRestart() {
    long before = revision();
    categoryService.initializeData();
    assertEquals(before, revision());
    assertTrue(states.findById(1L).orElseThrow().isInitialized());
  }

  @Test
  void duplicateAndUnknownCategoriesRejectedWithoutChangingRevision() {
    long before = revision();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            layoutService.reorder(
                request(List.of(categoryId, categoryId, defaultId), groups(List.of()))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            layoutService.reorder(
                request(List.of(categoryId, Long.MAX_VALUE, defaultId), groups(List.of()))));
    assertEquals(before, revision());
  }

  @Test
  void duplicateGroupsNullAndMissingArchiveRejected() {
    add("one");
    long before = revision();
    assertThrows(
        IllegalArgumentException.class,
        () -> layoutService.reorder(request(categoryIds(), groups(List.of()))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            layoutService.reorder(
                request(
                    categoryIds(),
                    List.of(
                        new ArchiveGroupOrder(categoryId, List.of()),
                        new ArchiveGroupOrder(categoryId, List.of()),
                        new ArchiveGroupOrder(defaultId, List.of())))));
    assertThrows(
        IllegalArgumentException.class,
        () -> layoutService.reorder(request(categoryIds(), Arrays.asList(null, null, null))));
    assertEquals(before, revision());
    assertEquals(1, archives.count());
  }

  @Test
  void duplicateArchiveAndDefaultFirstRejected() {
    add("one");
    Long id = archives.findAll().get(0).getId();
    long before = revision();
    assertThrows(
        IllegalArgumentException.class,
        () -> layoutService.reorder(request(categoryIds(), groups(List.of(id, id)))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            layoutService.reorder(
                request(List.of(defaultId, categoryId, otherId), groups(List.of(id)))));
    assertEquals(before, revision());
  }

  @Test
  void moveReordersAndPersists() {
    add("one");
    add("two");
    var all = archives.findAllInDisplayOrder();
    Long first = all.get(0).getId(), second = all.get(1).getId();
    layoutService.reorder(
        request(
            categoryIds(),
            List.of(
                new ArchiveGroupOrder(categoryId, List.of(second)),
                new ArchiveGroupOrder(otherId, List.of(first)),
                new ArchiveGroupOrder(defaultId, List.of()))));
    assertEquals(second, archives.findByCategoryIdInDisplayOrder(categoryId).get(0).getId());
    assertEquals(first, archives.findByCategoryIdInDisplayOrder(otherId).get(0).getId());
    assertEquals(0, archives.findByCategoryIdInDisplayOrder(otherId).get(0).getSortOrder());
  }

  @Test
  void staleSnapshotCannotOverwriteNewItem() {
    long old = revision();
    add("new");
    assertThrows(StaleRevisionException.class, () -> categoryService.create("stale", old));
    assertThrows(
        StaleRevisionException.class,
        () -> layoutService.reorder(new LayoutOrderRequest(old, categoryIds(), groups(List.of()))));
    assertEquals(1, archives.count());
    assertFalse(categories.existsByName("stale"));
  }

  @Test
  void invalidInputRollsBackRevision() {
    long before = revision();
    assertThrows(
        IllegalArgumentException.class,
        () -> archiveService.create(categoryId, "title", "https://", "memo", before));
    assertEquals(before, revision());
  }

  @Test
  void categoryDeletionMovesAllItemsToDefault() {
    add("one");
    add("two");
    categoryService.delete(categoryId, revision());
    assertFalse(categories.existsById(categoryId));
    var items = archives.findByCategoryIdInDisplayOrder(defaultId);
    assertEquals(2, items.size());
    assertEquals(1, items.get(1).getSortOrder());
  }

  @Test
  void pagingSearchAndLiteralWildcards() {
    for (int i = 0; i < 6; i++) add("Java " + i);
    add("100% coverage");
    var first = views.load(Map.of());
    var second = views.load(Map.of("p" + categoryId, "2"));
    assertEquals(4, items(first).get(categoryId).size());
    assertEquals(3, items(second).get(categoryId).size());
    var searched = views.load(Map.of("keyword", "%"));
    assertEquals(1L, searched.get("searchResultCount"));
    assertEquals("100% coverage", items(searched).get(categoryId).get(0).getTitle());
    assertEquals(7, items(views.load(Map.of("edit", "true"))).get(categoryId).size());
  }

  @SuppressWarnings("unchecked")
  Map<Long, List<Archive>> items(Map<String, Object> model) {
    return (Map<Long, List<Archive>>) model.get("archivesByCategory");
  }

  @Test
  void concurrentWritesFromSameRevisionHaveOnlyOneWinner() throws Exception {
    long old = revision();
    var pool = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    Callable<Boolean> task =
        () -> {
          ready.countDown();
          start.await();
          try {
            archiveService.create(categoryId, "race", "https://example.com", "", old);
            return true;
          } catch (StaleRevisionException e) {
            return false;
          }
        };
    try {
      var a = pool.submit(task);
      var b = pool.submit(task);
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
      assertEquals(1, archives.count());
      assertEquals(old + 1, revision());
    } finally {
      start.countDown();
      pool.shutdownNow();
    }
  }
}
