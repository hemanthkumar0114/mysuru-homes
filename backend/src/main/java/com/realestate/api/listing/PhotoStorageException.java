package com.realestate.api.listing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A photo failed validation (wrong type, too big, too many) or couldn't be saved. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PhotoStorageException extends RuntimeException {
    public PhotoStorageException(String message) {
        super(message);
    }
}
