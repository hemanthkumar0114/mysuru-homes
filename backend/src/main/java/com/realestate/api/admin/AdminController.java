package com.realestate.api.admin;

import com.realestate.api.security.AuthenticatedUser;
import com.realestate.api.visit.AdminVisitView;
import com.realestate.api.visit.VisitBookingService;
import com.realestate.api.visit.VisitStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Everything here requires the ADMIN role (enforced in SecurityConfig, not per-method). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminListingService adminListingService;
    private final VisitBookingService visitBookingService;

    @GetMapping("/listings/pending")
    public List<AdminListingSummary> pending() {
        return adminListingService.pending();
    }

    @PostMapping("/listings/{id}/verify")
    public AdminListingSummary verify(@PathVariable String id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return adminListingService.verify(id, principal.id());
    }

    /** Every visit request, newest first. ?status=REQUESTED narrows it to one status. */
    @GetMapping("/visits")
    public List<AdminVisitView> visits(@RequestParam(required = false) VisitStatus status) {
        return visitBookingService.listForAdmin(status);
    }

    /** Only a fresh request (REQUESTED) can be confirmed. */
    @PostMapping("/visits/{id}/confirm")
    public AdminVisitView confirmVisit(@PathVariable String id) {
        return visitBookingService.confirm(id);
    }

    /** A request that is still open (new or confirmed) can be cancelled. */
    @PostMapping("/visits/{id}/cancel")
    public AdminVisitView cancelVisit(@PathVariable String id) {
        return visitBookingService.cancelAsAdmin(id);
    }
}
