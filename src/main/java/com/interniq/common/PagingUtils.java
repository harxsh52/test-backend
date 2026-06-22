package com.interniq.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public final class PagingUtils {

    private PagingUtils() {
    }

    public static <T> Page<T> paginate(List<T> records, Pageable pageable) {
        int start = Math.min((int) pageable.getOffset(), records.size());
        int end = Math.min(start + pageable.getPageSize(), records.size());
        return new PageImpl<>(records.subList(start, end), pageable, records.size());
    }
}
