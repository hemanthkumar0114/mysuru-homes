package com.realestate.api.listing;

import com.realestate.api.security.AuthenticatedUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Uploading photos for one of your own listings. Up to 5 photos per listing in total. */
@RestController
@RequiredArgsConstructor
public class ListingPhotoController {

    private final ListingPhotoService listingPhotoService;

    @PostMapping("/api/listings/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<String> upload(
            @PathVariable String id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return listingPhotoService.upload(id, principal.id(), files);
    }
}
