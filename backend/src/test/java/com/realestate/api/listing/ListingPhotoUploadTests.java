package com.realestate.api.listing;

import static org.assertj.core.api.Assertions.assertThat;

import com.realestate.api.support.ApiTestSupport;
import com.realestate.api.user.UserRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class ListingPhotoUploadTests extends ApiTestSupport {

    @Autowired
    private ListingPhotoRepository listingPhotoRepository;

    @Test
    void photosUploadedTogetherGetDistinctIncreasingSortOrders() {
        String owner = registerUser(UserRole.OWNER).token();
        String listingId = createListing(owner, Map.of()).id();

        ResponseEntity<List> res = upload(owner, listingId, 3);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(listingPhotoRepository.findByListingIdOrderBySortOrderAsc(listingId))
                .extracting(ListingPhoto::getSortOrder)
                .containsExactly(0, 1, 2);
    }

    @Test
    void laterUploadsContinueAfterTheExistingPhotos() {
        String owner = registerUser(UserRole.OWNER).token();
        String listingId = createListing(owner, Map.of()).id();

        upload(owner, listingId, 2);
        upload(owner, listingId, 2);

        assertThat(listingPhotoRepository.findByListingIdOrderBySortOrderAsc(listingId))
                .extracting(ListingPhoto::getSortOrder)
                .containsExactly(0, 1, 2, 3);
    }

    private ResponseEntity<List> upload(String token, String listingId, int count) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        for (int i = 0; i < count; i++) {
            parts.add("files", imagePart("photo-" + i + ".png"));
        }
        HttpHeaders requestHeaders = headers(token);
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.exchange(
                "/api/listings/" + listingId + "/photos",
                HttpMethod.POST,
                new HttpEntity<>(parts, requestHeaders),
                List.class);
    }

    private static HttpEntity<ByteArrayResource> imagePart(String filename) {
        ByteArrayResource resource =
                new ByteArrayResource(new byte[] {1, 2, 3}) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        return new HttpEntity<>(resource, partHeaders);
    }
}
