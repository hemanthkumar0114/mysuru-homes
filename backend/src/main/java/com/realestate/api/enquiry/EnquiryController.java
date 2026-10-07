package com.realestate.api.enquiry;

import com.realestate.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    public record CreateEnquiryRequest(@NotBlank(message = "is required") String listingId) {}

    /** Any logged-in user can enquire - the tenant is taken from the JWT, not the request body. */
    @PostMapping("/api/enquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody CreateEnquiryRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        enquiryService.create(request.listingId(), principal.id());
    }
}
