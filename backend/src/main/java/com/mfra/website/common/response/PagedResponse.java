package com.mfra.website.common.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PagedResponse<T> {

    private boolean success;
    private List<T> data;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private Instant timestamp;

    public static <T> PagedResponse<T> of(List<T> data, int page, int size,
                                           long totalElements, int totalPages) {
        return PagedResponse.<T>builder()
                .success(true)
                .data(data)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .timestamp(Instant.now())
                .build();
    }
}
