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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds accounts (and, for the demo owner, a couple of sample listings) on first run, so a
 * fresh deployment isn't completely empty. Gated by app.seed-demo-data and by "does the users
 * table already have anything in it" so it never re-seeds or runs against a database that
 * already has real data.
 *
 * There is no hardcoded password anywhere in this class - every account it creates comes
 * from an env var pair (see README / DEPLOY.md), and any pair left unset simply means that
 * account isn't created. This is deliberate: a public deployment must not ship a guessable
 * admin login.
 */
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.demo-owner.email:}")
    private String demoOwnerEmail;

    @Value("${app.demo-owner.password:}")
    private String demoOwnerPassword;

    @Value("${app.demo-tenant.email:}")
    private String demoTenantEmail;

    @Value("${app.demo-tenant.password:}")
    private String demoTenantPassword;

    @Override
    public void run(String... args) {
        if (!seedDemoData || userRepository.count() > 0) {
            return;
        }

        User admin = null;
        if (isSet(adminEmail) && isSet(adminPassword)) {
            admin = userRepository.save(demoUser("Admin", adminEmail, adminPassword, UserRole.ADMIN));
        } else {
            log.warn("ADMIN_EMAIL/ADMIN_PASSWORD not set - skipping admin account creation.");
        }

        User owner = null;
        if (isSet(demoOwnerEmail) && isSet(demoOwnerPassword)) {
            owner = userRepository.save(demoUser("Demo Owner", demoOwnerEmail, demoOwnerPassword, UserRole.OWNER));
        }

        if (isSet(demoTenantEmail) && isSet(demoTenantPassword)) {
            userRepository.save(demoUser("Demo Tenant", demoTenantEmail, demoTenantPassword, UserRole.TENANT));
        }

        // Sample listings need both an owner and an admin to verify them, so they're only
        // seeded when both of those accounts actually got created above.
        if (owner != null && admin != null) {
            seedSampleListings(owner, admin);
        }
    }

    private void seedSampleListings(User owner, User admin) {
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

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
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
