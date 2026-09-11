package com.realestate.api.visit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitBookingRepository extends JpaRepository<VisitBooking, String> {
    List<VisitBooking> findByStatus(VisitStatus status);
}
