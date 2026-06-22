package com.interniq.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageRequestFactory {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageRequestFactory() {
    }

    public static Pageable create(Integer page, Integer size, String sortBy, String sortDirection, Set<String> allowedSortFields, String defaultSortBy) {
        String safeSortBy = allowedSortFields.contains(sortBy) ? sortBy : defaultSortBy;
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    public static boolean isPaged(Integer page, Integer size) {
        return page != null || size != null;
    }
}
