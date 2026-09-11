package com.realestate.api.config;

import com.realestate.api.listing.Listing;
import com.realestate.api.listing.ListingRepository;
import com.realestate.api.listing.ListingStatus;
import com.realestate.api.listing.ListingType;
import com.realestate.api.user.User;
import com.realestate.api.user.UserRepository;
import com.realestate.api.user.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a handful of sample listings so the "local" profile (H2, no Postgres
 * needed) has something to look at. Gated by app.seed-demo-data so it never
 * runs against the real Postgres profile by accident.
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    @Override
    public void run(String... args) {
        if (!seedDemoData || listingRepository.count() > 0) {
            return;
        }

        User owner =
                userRepository.save(
                        User.builder()
                                .phone("+91 90000 00001")
                                .name("Demo Owner")
                                .role(UserRole.OWNER)
                                .build());

        User fieldAgent =
                userRepository.save(
                        User.builder()
                                .phone("+91 90000 00002")
                                .name("Demo Field Agent")
                                .role(UserRole.FIELD_AGENT)
                                .build());

        listingRepository.save(
                Listing.builder()
                        .owner(owner)
                        .type(ListingType.RENT)
                        .status(ListingStatus.LIVE)
                        .title("2BHK near Infosys Mysuru campus")
                        .addressLine("Hebbal Industrial Area, Mysuru")
                        .locality("Hebbal")
                        .lat(12.3403)
                        .lng(76.6197)
                        .rentAmount(new BigDecimal("14000"))
                        .bedrooms(2)
                        .bathrooms(2)
                        .verifiedAt(Instant.now())
                        .verifiedBy(fieldAgent)
                        .lastConfirmedAt(Instant.now())
                        .build());

        listingRepository.save(
                Listing.builder()
                        .owner(owner)
                        .type(ListingType.PG)
                        .status(ListingStatus.LIVE)
                        .title("PG for working women, walk to Vijayanagar 2nd Stage")
                        .addressLine("Vijayanagar 2nd Stage, Mysuru")
                        .locality("Vijayanagar")
                        .lat(12.3244)
                        .lng(76.6183)
                        .rentAmount(new BigDecimal("7500"))
                        .bedrooms(1)
                        .verifiedAt(Instant.now())
                        .verifiedBy(fieldAgent)
                        .lastConfirmedAt(Instant.now())
                        .build());

        listingRepository.save(
                Listing.builder()
                        .owner(owner)
                        .type(ListingType.RENT)
                        .status(ListingStatus.LIVE)
                        .title("1BHK independent house, Bogadi Road")
                        .addressLine("Bogadi Road, Mysuru")
                        .locality("Bogadi")
                        .lat(12.2953)
                        .lng(76.6011)
                        .rentAmount(new BigDecimal("9000"))
                        .bedrooms(1)
                        .bathrooms(1)
                        .build());
    }
}
