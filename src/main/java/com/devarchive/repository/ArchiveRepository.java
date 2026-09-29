package com.devarchive.repository;

import com.devarchive.domain.Archive;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchiveRepository extends JpaRepository<Archive, Long> {

  @Query(
      """
      select a
      from Archive a
      join fetch a.category
      order by
          a.category.id asc,
          coalesce(a.sortOrder, 2147483647) asc,
          a.id asc
      """)
  List<Archive> findAllInDisplayOrder();

  @Query(
      """
      select a
      from Archive a
      where a.category.id = :categoryId
      order by
          coalesce(a.sortOrder, 2147483647) asc,
          a.id asc
      """)
  List<Archive> findByCategoryIdInDisplayOrder(@Param("categoryId") Long categoryId);

  @Query(
      """
      select coalesce(max(a.sortOrder), -1)
      from Archive a
      where a.category.id = :categoryId
      """)
  int findMaxSortOrderByCategoryId(@Param("categoryId") Long categoryId);

  @Query(
      value =
          "select a from Archive a join fetch a.category where a.category.id = :categoryId and"
              + " (lower(a.title) like :pattern escape '!' or lower(a.url) like :pattern escape '!'"
              + " or lower(coalesce(a.memo, '')) like :pattern escape '!') order by"
              + " coalesce(a.sortOrder,2147483647), a.id",
      countQuery =
          "select count(a) from Archive a where a.category.id = :categoryId and (lower(a.title)"
              + " like :pattern escape '!' or lower(a.url) like :pattern escape '!' or"
              + " lower(coalesce(a.memo, '')) like :pattern escape '!')")
  org.springframework.data.domain.Page<Archive> pageByCategory(
      @Param("categoryId") Long categoryId,
      @Param("pattern") String pattern,
      org.springframework.data.domain.Pageable pageable);
}
