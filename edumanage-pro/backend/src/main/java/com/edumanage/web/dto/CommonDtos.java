package com.edumanage.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class CommonDtos {
    private CommonDtos() {}

    public record PageResponse<T>(List<T> items, int page, int size,
                                  long totalItems, int totalPages, boolean hasNext) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.hasNext());
        }

        public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
            return from(page.map(mapper));
        }
    }
}