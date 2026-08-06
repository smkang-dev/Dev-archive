package com.devarchive.repository;

import com.devarchive.domain.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArchiveRepository
        extends JpaRepository<Archive, Long> {

    @Query("""
            select a
            from Archive a
            join fetch a.category
            order by
                a.category.id asc,
                coalesce(a.sortOrder, 2147483647) asc,
                a.id asc
            """)
    List<Archive> findAllInDisplayOrder();

    @Query("""
            select a
            from Archive a
            where a.category.id = :categoryId
            order by
                coalesce(a.sortOrder, 2147483647) asc,
                a.id asc
            """)
    List<Archive> findByCategoryIdInDisplayOrder(
            @Param("categoryId") Long categoryId
    );

    @Query("""
            select coalesce(max(a.sortOrder), -1)
            from Archive a
            where a.category.id = :categoryId
            """)
    int findMaxSortOrderByCategoryId(
            @Param("categoryId") Long categoryId
    );

    @Query("""
            select a
            from Archive a
            join fetch a.category
            where
                lower(a.title)
                    like lower(concat('%', :keyword, '%'))
                or lower(a.url)
                    like lower(concat('%', :keyword, '%'))
                or lower(coalesce(a.memo, ''))
                    like lower(concat('%', :keyword, '%'))
            order by
                coalesce(a.sortOrder, 2147483647) asc,
                a.id asc
            """)
    List<Archive> search(
            @Param("keyword") String keyword
    );
}