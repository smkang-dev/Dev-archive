package com.devarchive.service;

import com.devarchive.domain.Archive;
import com.devarchive.domain.Category;
import com.devarchive.repository.ArchiveRepository;
import com.devarchive.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final String DEFAULT_CATEGORY_NAME = "기타";

    private final CategoryRepository categoryRepository;
    private final ArchiveRepository archiveRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ArchiveRepository archiveRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.archiveRepository = archiveRepository;
    }

    @PostConstruct
    @Transactional
    public void createDefaultCategory() {
        if (!categoryRepository.existsByName(DEFAULT_CATEGORY_NAME)) {
            categoryRepository.save(
                    new Category(DEFAULT_CATEGORY_NAME, true)
            );
        }
    }

    public List<Category> findAll() {
        return categoryRepository
                .findAllByOrderByDefaultCategoryAscIdAsc();
    }

    @Transactional
    public void create(String name) {
        String trimmedName = name == null ? "" : name.trim();

        if (trimmedName.isBlank()) {
            throw new IllegalArgumentException(
                    "카테고리 이름을 입력해 주세요."
            );
        }

        if (trimmedName.length() > 30) {
            throw new IllegalArgumentException(
                    "카테고리 이름은 30자 이하로 입력해 주세요."
            );
        }

        if (categoryRepository.existsByName(trimmedName)) {
            throw new IllegalArgumentException(
                    "이미 존재하는 카테고리입니다."
            );
        }

        categoryRepository.save(
                new Category(trimmedName, false)
        );
    }

    @Transactional
    public void delete(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 카테고리입니다."
                        )
                );

        if (category.isDefaultCategory()) {
            throw new IllegalArgumentException(
                    "기타 카테고리는 삭제할 수 없습니다."
            );
        }

        Category defaultCategory = categoryRepository
                .findByName(DEFAULT_CATEGORY_NAME)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "기타 카테고리가 존재하지 않습니다."
                        )
                );

        List<Archive> archives =
                archiveRepository.findByCategoryId(categoryId);

        for (Archive archive : archives) {
            archive.moveTo(defaultCategory);
        }

        categoryRepository.delete(category);
    }
}