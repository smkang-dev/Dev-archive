package com.devarchive.controller;

import java.util.List;

public record LayoutOrderRequest(
        List<Long> categoryIds,
        List<ArchiveGroupOrder> archiveGroups
) {

    public record ArchiveGroupOrder(
            Long categoryId,
            List<Long> archiveIds
    ) {
    }
}