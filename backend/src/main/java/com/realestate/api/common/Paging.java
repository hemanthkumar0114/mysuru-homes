package com.realestate.api.common;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * One place for the page/size rules every list endpoint shares. Responses stay plain JSON
 * arrays; the total number of matches travels in the X-Total-Count header.
 */
public final class Paging {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;
    public static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    private Paging() {}

    public static Pageable of(Integer page, Integer size) {
        return of(page, size, Sort.unsorted());
    }

    public static Pageable of(Integer page, Integer size, Sort sort) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be 0 or more.");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and " + MAX_SIZE + ".");
        }
        return PageRequest.of(pageNumber, pageSize, sort);
    }

    public static <T> ResponseEntity<List<T>> response(Page<T> page) {
        return ResponseEntity.ok().header(TOTAL_COUNT_HEADER, String.valueOf(page.getTotalElements())).body(page.getContent());
    }
}
