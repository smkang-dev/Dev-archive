package com.devarchive.service;

import com.devarchive.controller.LayoutOrderRequest;
import com.devarchive.domain.Archive;
import com.devarchive.domain.Category;
import com.devarchive.repository.ArchiveRepository;
import com.devarchive.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LayoutService {

    private final CategoryRepository categoryRepository;
    private final ArchiveRepository archiveRepository;

    public LayoutService(
            CategoryRepository categoryRepository,
            ArchiveRepository archiveRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.archiveRepository = archiveRepository;
    }

    @Transactional
    public void reorder(LayoutOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "정렬 정보가 없습니다."
            );
        }

        reorderCategories(request.categoryIds());
        reorderArchives(request.archiveGroups());
    }

    private void reorderCategories(
            List<Long> categoryIds
    ) {
        if (categoryIds == null) {
            throw new IllegalArgumentException(
                    "카테고리 순서가 없습니다."
            );
        }

        List<Category> categories =
                categoryRepository.findAll();

        Map<Long, Category> categoryMap =
                categories.stream()
                        .collect(Collectors.toMap(
                                Category::getId,
                                Function.identity()
                        ));

        Set<Long> expectedUserIds =
                categories.stream()
                        .filter(category ->
                                !category.isDefaultCategory()
                        )
                        .map(Category::getId)
                        .collect(Collectors.toSet());

        List<Long> requestedUserIds =
                categoryIds.stream()
                        .filter(categoryMap::containsKey)
                        .filter(id ->
                                !categoryMap
                                        .get(id)
                                        .isDefaultCategory()
                        )
                        .toList();

        if (!new HashSet<>(requestedUserIds)
                .equals(expectedUserIds)) {
            throw new IllegalArgumentException(
                    "카테고리 정렬 정보가 올바르지 않습니다."
            );
        }

        for (int i = 0;
             i < requestedUserIds.size();
             i++) {

            categoryMap
                    .get(requestedUserIds.get(i))
                    .changeSortOrder(i);
        }
    }

    private void reorderArchives(
            List<LayoutOrderRequest.ArchiveGroupOrder>
                    archiveGroups
    ) {
        if (archiveGroups == null) {
            throw new IllegalArgumentException(
                    "자료 순서가 없습니다."
            );
        }

        Map<Long, Category> categoryMap =
                categoryRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                Category::getId,
                                Function.identity()
                        ));

        List<Archive> archives =
                archiveRepository.findAll();

        Map<Long, Archive> archiveMap =
                archives.stream()
                        .collect(Collectors.toMap(
                                Archive::getId,
                                Function.identity()
                        ));

        Set<Long> receivedArchiveIds =
                new HashSet<>();

        for (LayoutOrderRequest.ArchiveGroupOrder group
                : archiveGroups) {

            Category category =
                    categoryMap.get(group.categoryId());

            if (category == null) {
                throw new IllegalArgumentException(
                        "존재하지 않는 카테고리가 포함되어 있습니다."
                );
            }

            List<Long> archiveIds =
                    group.archiveIds() == null
                            ? List.of()
                            : group.archiveIds();

            for (int i = 0;
                 i < archiveIds.size();
                 i++) {

                Long archiveId = archiveIds.get(i);
                Archive archive =
                        archiveMap.get(archiveId);

                if (archive == null
                        || !receivedArchiveIds.add(archiveId)) {
                    throw new IllegalArgumentException(
                            "자료 정렬 정보가 올바르지 않습니다."
                    );
                }

                archive.moveTo(category);
                archive.changeSortOrder(i);
            }
        }

        if (!receivedArchiveIds.equals(
                archiveMap.keySet()
        )) {
            throw new IllegalArgumentException(
                    "일부 자료의 정렬 정보가 누락되었습니다."
            );
        }
    }
}