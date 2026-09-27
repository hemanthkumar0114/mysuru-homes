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
import org.springframework.web.server.ResponseStatusException;

/** Uploading photos for one of your own listings. Up to 5 photos per listing in total. */
@RestController
@RequiredArgsConstructor
public class ListingPhotoController {

    private final ListingRepository listingRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final PhotoStorageService photoStorageService;

    @PostMapping("/api/listings/{id}/photos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<String> upload(
            @PathVariable String id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Listing listing =
                listingRepository
                        .findByIdAndOwnerId(id, principal.id())
                        .orElseThrow(() -> new ListingNotFoundException(id));

        if (files.isEmpty()) {
            throw new PhotoStorageException("Please choose at least one photo.");
        }

        long existing = listingPhotoRepository.countByListingId(id);
        if (existing + files.size() > PhotoStorageService.MAX_PHOTOS_PER_LISTING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A listing can have at most " + PhotoStorageService.MAX_PHOTOS_PER_LISTING + " photos ("
                            + existing + " already uploaded).");
        }

        int nextOrder = (int) existing;
        return files.stream()
                .map(
                        file -> {
                            String url = photoStorageService.store(id, file);
                            listingPhotoRepository.save(
                                    ListingPhoto.builder().listing(listing).url(url).sortOrder(nextOrder).build());
                            return url;
                        })
                .toList();
    }
}
