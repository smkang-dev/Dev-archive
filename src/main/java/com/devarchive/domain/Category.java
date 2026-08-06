package com.devarchive.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private boolean defaultCategory;

    /*
     * 기존 데이터에도 안전하게 컬럼을 추가하기 위해
     * 일단 Integer와 nullable 컬럼으로 둔다.
     * 서버 시작 시 Service에서 기존 순서를 채운다.
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    protected Category() {
    }

    public Category(
            String name,
            boolean defaultCategory,
            Integer sortOrder
    ) {
        this.name = name;
        this.defaultCategory = defaultCategory;
        this.sortOrder = sortOrder;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaultCategory() {
        return defaultCategory;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}