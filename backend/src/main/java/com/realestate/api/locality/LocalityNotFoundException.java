package com.realestate.api.locality;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LocalityNotFoundException extends RuntimeException {
    public LocalityNotFoundException(String slug) {
        super("We couldn't find that locality.");
    }
}
