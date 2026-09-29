package com.devarchive.controller;

import com.devarchive.service.ArchiveService;
import com.devarchive.service.ArchiveViewService;
import com.devarchive.service.CategoryService;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

  private final CategoryService categoryService;
  private final ArchiveService archiveService;

  private final ArchiveViewService viewService;

  public HomeController(
      CategoryService categoryService,
      ArchiveService archiveService,
      ArchiveViewService viewService) {
    this.categoryService = categoryService;
    this.archiveService = archiveService;
    this.viewService = viewService;
  }

  @GetMapping("/")
  public String home(@RequestParam Map<String, String> params, Model model) {
    model.addAllAttributes(viewService.load(params));
    return "index";
  }

  @PostMapping("/categories")
  public String createCategory(@RequestParam String name, @RequestParam Long expectedRevision) {
    categoryService.create(name, expectedRevision);
    return "redirect:/";
  }

  @PostMapping("/categories/{id}/delete")
  public String deleteCategory(@PathVariable Long id, @RequestParam Long expectedRevision) {
    categoryService.delete(id, expectedRevision);
    return "redirect:/";
  }

  @PostMapping("/archives")
  public String createArchive(
      @RequestParam Long categoryId,
      @RequestParam String title,
      @RequestParam String url,
      @RequestParam(required = false) String memo,
      @RequestParam Long expectedRevision) {
    archiveService.create(categoryId, title, url, memo, expectedRevision);

    return "redirect:/";
  }

  @PostMapping("/archives/{id}/update")
  public String updateArchive(
      @PathVariable Long id,
      @RequestParam Long categoryId,
      @RequestParam String title,
      @RequestParam String url,
      @RequestParam(required = false) String memo,
      @RequestParam Long expectedRevision) {
    archiveService.update(id, categoryId, title, url, memo, expectedRevision);

    return "redirect:/";
  }

  @PostMapping("/archives/{id}/delete")
  public String deleteArchive(@PathVariable Long id, @RequestParam Long expectedRevision) {
    archiveService.delete(id, expectedRevision);
    return "redirect:/";
  }
}
