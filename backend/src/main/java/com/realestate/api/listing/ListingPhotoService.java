package com.realestate.api.listing;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListingPhotoService {

    private final ListingRepository listingRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final PhotoStorageService photoStorageService;

    /**
     * Every file is validated before any is stored, so a bad file in the batch doesn't leave
     * the earlier ones behind as orphans. The rows are then saved together. Storage is
     * deliberately outside a database transaction so a slow upload never holds a connection.
     */
    public List<String> upload(String listingId, String ownerId, List<MultipartFile> files) {
        Listing listing =
                listingRepository
                        .findByIdAndOwnerId(listingId, ownerId)
                        .orElseThrow(() -> new ListingNotFoundException(listingId));

        if (files.isEmpty()) {
            throw new PhotoStorageException("Please choose at least one photo.");
        }

        long existing = listingPhotoRepository.countByListingId(listingId);
        if (existing + files.size() > PhotoStorageService.MAX_PHOTOS_PER_LISTING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A listing can have at most " + PhotoStorageService.MAX_PHOTOS_PER_LISTING + " photos ("
                            + existing + " already uploaded).");
        }

        files.forEach(photoStorageService::validate);

        List<String> urls = new ArrayList<>();
        List<ListingPhoto> photos = new ArrayList<>();
        int sortOrder = (int) existing;
        for (MultipartFile file : files) {
            String url = photoStorageService.store(listingId, file);
            urls.add(url);
            photos.add(ListingPhoto.builder().listing(listing).url(url).sortOrder(sortOrder++).build());
        }
        listingPhotoRepository.saveAll(photos);
        return urls;
    }
}
