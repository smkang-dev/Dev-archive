package com.devarchive.repository;

import com.devarchive.domain.Archive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveRepository extends JpaRepository<Archive, Long> {

    List<Archive> findAllByOrderByCreatedAtDesc();

    List<Archive> findByCategoryId(Long categoryId);

    List<Archive>
    findByTitleContainingIgnoreCaseOrUrlContainingIgnoreCaseOrMemoContainingIgnoreCaseOrderByCreatedAtDesc(
            String title,
            String url,
            String memo
    );
}