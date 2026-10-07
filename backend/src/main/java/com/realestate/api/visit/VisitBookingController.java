package com.realestate.api.visit;

import com.realestate.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VisitBookingController {

    private final VisitBookingService visitBookingService;

    /**
     * slotTime is an exact instant, so it must carry a zone: "2026-09-27T04:12:00Z"
     * or "2026-09-27T09:42:00+05:30". (The frontend converts the IST time the user
     * picked into this form.) Stored as UTC.
     */
    public record CreateVisitRequest(
            @NotBlank(message = "is required") String listingId,
            @NotNull(message = "is required") @Future(message = "must be in the future") Instant slotTime) {}

    @PostMapping("/api/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @Valid @RequestBody CreateVisitRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        visitBookingService.create(request.listingId(), request.slotTime(), principal.id());
    }

    /** The logged-in user's own visit requests. */
    @GetMapping("/api/visits/mine")
    public List<VisitView> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return visitBookingService.mine(principal.id());
    }

    /**
     * Cancel one of your own open requests. A request that belongs to someone else is
     * reported as "not found" - the same answer as an id that doesn't exist.
     */
    @PostMapping("/api/visits/{id}/cancel")
    public VisitView cancel(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return visitBookingService.cancelOwn(id, principal.id());
    }
}
