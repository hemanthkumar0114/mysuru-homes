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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts and listings on first run, so the app isn't empty the
 * moment you start it. Gated by app.seed-demo-data and by "does the users
 * table already have anything in it" so it never re-seeds or runs against
 * a database that already has real data.
 *
 * Demo logins (see README): admin@mysuruhomes.local / admin1234,
 * owner@mysuruhomes.local / owner1234.
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    @Override
    public void run(String... args) {
        if (!seedDemoData || userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(demoUser("Admin", "admin@mysuruhomes.local", "admin1234", UserRole.ADMIN));
        User owner = userRepository.save(demoUser("Demo Owner", "owner@mysuruhomes.local", "owner1234", UserRole.OWNER));
        userRepository.save(demoUser("Demo Tenant", "tenant@mysuruhomes.local", "tenant1234", UserRole.TENANT));

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
                        .verifiedBy(admin)
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
                        .verifiedBy(admin)
                        .lastConfirmedAt(Instant.now())
                        .build());

        // Left DRAFT on purpose - gives the admin moderation page something
        // to show right away without you having to post a listing first.
        listingRepository.save(
                Listing.builder()
                        .owner(owner)
                        .type(ListingType.RENT)
                        .status(ListingStatus.DRAFT)
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

    private User demoUser(String name, String email, String rawPassword, UserRole role) {
        return User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
    }
}
