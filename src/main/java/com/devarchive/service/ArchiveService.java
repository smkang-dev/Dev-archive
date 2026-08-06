package com.devarchive.service;

import com.devarchive.domain.Archive;
import com.devarchive.domain.Category;
import com.devarchive.repository.ArchiveRepository;
import com.devarchive.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final CategoryRepository categoryRepository;

    public ArchiveService(
            ArchiveRepository archiveRepository,
            CategoryRepository categoryRepository
    ) {
        this.archiveRepository = archiveRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Archive> findAll() {
        return archiveRepository.findAllInDisplayOrder();
    }

    public List<Archive> search(String keyword) {
        String value =
                keyword == null ? "" : keyword.trim();

        if (value.isBlank()) {
            return findAll();
        }

        return archiveRepository.search(value);
    }

    @Transactional
    public void create(
            Long categoryId,
            String title,
            String url,
            String memo
    ) {
        Category category = findCategory(categoryId);

        int nextOrder =
                archiveRepository
                        .findMaxSortOrderByCategoryId(categoryId)
                        + 1;

        archiveRepository.save(
                new Archive(
                        validateTitle(title),
                        validateUrl(url),
                        normalizeMemo(memo),
                        category,
                        nextOrder
                )
        );
    }

    @Transactional
    public void update(
            Long archiveId,
            Long categoryId,
            String title,
            String url,
            String memo
    ) {
        Archive archive = archiveRepository
                .findById(archiveId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 자료입니다."
                        )
                );

        Long previousCategoryId =
                archive.getCategory().getId();

        Category category = findCategory(categoryId);

        boolean categoryChanged =
                !previousCategoryId.equals(categoryId);

        archive.update(
                validateTitle(title),
                validateUrl(url),
                normalizeMemo(memo),
                category
        );

        if (categoryChanged) {
            int nextOrder =
                    archiveRepository
                            .findMaxSortOrderByCategoryId(categoryId)
                            + 1;

            archive.changeSortOrder(nextOrder);
            normalizeArchiveOrders(previousCategoryId);
        }
    }

    @Transactional
    public void delete(Long archiveId) {
        Archive archive = archiveRepository
                .findById(archiveId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 자료입니다."
                        )
                );

        Long categoryId =
                archive.getCategory().getId();

        archiveRepository.delete(archive);
        archiveRepository.flush();

        normalizeArchiveOrders(categoryId);
    }

    private void normalizeArchiveOrders(Long categoryId) {
        List<Archive> archives =
                archiveRepository
                        .findByCategoryIdInDisplayOrder(categoryId);

        for (int i = 0; i < archives.size(); i++) {
            archives.get(i).changeSortOrder(i);
        }
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 카테고리입니다."
                        )
                );
    }

    private String validateTitle(String title) {
        String value =
                title == null ? "" : title.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "제목을 입력해 주세요."
            );
        }

        if (value.length() > 100) {
            throw new IllegalArgumentException(
                    "제목은 100자 이하로 입력해 주세요."
            );
        }

        return value;
    }

    private String validateUrl(String url) {
        String value =
                url == null ? "" : url.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "URL을 입력해 주세요."
            );
        }

        if (!value.startsWith("http://")
                && !value.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "URL은 http:// 또는 https://로 시작해야 합니다."
            );
        }

        if (value.length() > 1000) {
            throw new IllegalArgumentException(
                    "URL이 너무 깁니다."
            );
        }

        return value;
    }

    private String normalizeMemo(String memo) {
        return memo == null ? "" : memo.trim();
    }
}