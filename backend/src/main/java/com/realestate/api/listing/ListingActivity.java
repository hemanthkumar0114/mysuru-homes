package com.realestate.api.listing;

import com.realestate.api.enquiry.Enquiry;
import com.realestate.api.visit.VisitBooking;
import com.realestate.api.visit.VisitStatus;
import java.time.Instant;
import java.util.List;

/**
 * Everything that has happened on one listing, for its owner. Tenants appear
 * by first name only - contact details stay with the platform, which
 * coordinates visits through the admin team.
 */
public record ListingActivity(
        String listingId, String title, List<EnquiryItem> enquiries, List<VisitItem> visits) {

    public record EnquiryItem(String id, String tenantName, Instant createdAt) {
        static EnquiryItem from(Enquiry enquiry) {
            return new EnquiryItem(enquiry.getId(), firstName(enquiry.getTenant().getName()), enquiry.getCreatedAt());
        }
    }

    public record VisitItem(String id, String tenantName, Instant slotTime, VisitStatus status, Instant createdAt) {
        static VisitItem from(VisitBooking visit) {
            return new VisitItem(
                    visit.getId(),
                    firstName(visit.getTenant().getName()),
                    visit.getSlotTime(),
                    visit.getStatus(),
                    visit.getCreatedAt());
        }
    }

    static String firstName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }
}
