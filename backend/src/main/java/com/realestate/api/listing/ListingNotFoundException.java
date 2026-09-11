package com.realestate.api.listing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ListingNotFoundException extends RuntimeException {
    public ListingNotFoundException(String id) {
        super("Listing not found: " + id);
    }
}
