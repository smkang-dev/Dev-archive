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
        return archiveRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void create(
            Long categoryId,
            String title,
            String url,
            String memo
    ) {
        Category category = findCategory(categoryId);

        archiveRepository.save(
                new Archive(
                        validateTitle(title),
                        validateUrl(url),
                        normalizeMemo(memo),
                        category
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
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 자료입니다.")
                );

        Category category = findCategory(categoryId);

        archive.update(
                validateTitle(title),
                validateUrl(url),
                normalizeMemo(memo),
                category
        );
    }

    @Transactional
    public void delete(Long archiveId) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 자료입니다.")
                );

        archiveRepository.delete(archive);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 카테고리입니다.")
                );
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

        if (!value.startsWith("http://") &&
                !value.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "URL은 http:// 또는 https://로 시작해야 합니다."
            );
        }

        if (value.length() > 1000) {
            throw new IllegalArgumentException("URL이 너무 깁니다.");
        }

        return value;
    }

    private String normalizeMemo(String memo) {
        return memo == null ? "" : memo.trim();
    }
}