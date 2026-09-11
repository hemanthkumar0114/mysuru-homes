package com.realestate.api.enquiry;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepository extends JpaRepository<Enquiry, String> {
    List<Enquiry> findByListingIdAndCreatedAtAfter(String listingId, Instant since);
}
