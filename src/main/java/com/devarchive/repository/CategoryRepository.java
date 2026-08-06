package com.devarchive.repository;

import com.devarchive.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    Optional<Category> findByName(String name);

    @Query("""
            select c
            from Category c
            order by
                c.defaultCategory asc,
                coalesce(c.sortOrder, 2147483647) asc,
                c.id asc
            """)
    List<Category> findAllInDisplayOrder();

    @Query("""
            select coalesce(max(c.sortOrder), -1)
            from Category c
            where c.defaultCategory = false
            """)
    int findMaxUserSortOrder();
}