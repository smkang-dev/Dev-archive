package com.devarchive.controller;

import com.devarchive.domain.Archive;
import com.devarchive.service.ArchiveService;
import com.devarchive.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final CategoryService categoryService;
    private final ArchiveService archiveService;

    public HomeController(
            CategoryService categoryService,
            ArchiveService archiveService
    ) {
        this.categoryService = categoryService;
        this.archiveService = archiveService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Archive> archives = archiveService.search(keyword);

        Map<Long, List<Archive>> archivesByCategory =
                archives.stream()
                        .collect(
                                Collectors.groupingBy(
                                        archive ->
                                                archive.getCategory().getId()
                                )
                        );

        boolean searching =
                keyword != null && !keyword.trim().isBlank();

        model.addAttribute(
                "categories",
                categoryService.findAll()
        );

        model.addAttribute(
                "archivesByCategory",
                archivesByCategory
        );

        model.addAttribute(
                "keyword",
                keyword == null ? "" : keyword.trim()
        );

        model.addAttribute(
                "searching",
                searching
        );

        model.addAttribute(
                "searchResultCount",
                archives.size()
        );

        return "index";
    }

    @PostMapping("/categories")
    public String createCategory(
            @RequestParam String name
    ) {
        categoryService.create(name);
        return "redirect:/";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(
            @PathVariable Long id
    ) {
        categoryService.delete(id);
        return "redirect:/";
    }

    @PostMapping("/archives")
    public String createArchive(
            @RequestParam Long categoryId,
            @RequestParam String title,
            @RequestParam String url,
            @RequestParam(required = false) String memo
    ) {
        archiveService.create(
                categoryId,
                title,
                url,
                memo
        );

        return "redirect:/";
    }

    @PostMapping("/archives/{id}/update")
    public String updateArchive(
            @PathVariable Long id,
            @RequestParam Long categoryId,
            @RequestParam String title,
            @RequestParam String url,
            @RequestParam(required = false) String memo
    ) {
        archiveService.update(
                id,
                categoryId,
                title,
                url,
                memo
        );

        return "redirect:/";
    }

    @PostMapping("/archives/{id}/delete")
    public String deleteArchive(
            @PathVariable Long id
    ) {
        archiveService.delete(id);
        return "redirect:/";
    }
}