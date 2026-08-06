package com.devarchive.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "archives")
public class Archive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected Archive() {
    }

    public Archive(
            String title,
            String url,
            String memo,
            Category category,
            Integer sortOrder
    ) {
        this.title = title;
        this.url = url;
        this.memo = memo;
        this.category = category;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            String title,
            String url,
            String memo,
            Category category
    ) {
        this.title = title;
        this.url = url;
        this.memo = memo;
        this.category = category;
    }

    public void moveTo(Category category) {
        this.category = category;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getMemo() {
        return memo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Category getCategory() {
        return category;
    }
}