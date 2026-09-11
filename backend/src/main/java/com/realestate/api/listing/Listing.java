package com.realestate.api.listing;

import com.realestate.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single rental / PG unit. Scoped to Phase 1: owner-direct residential
 * rentals and PG/co-living only, within one Mysuru corridor. Resale, plots
 * and commercial are out of scope until Phase 2 - do not add fields for
 * them here without revisiting the phase plan.
 */
@Entity
@Table(
        name = "listings",
        indexes = {
            @Index(name = "idx_listing_status", columnList = "status"),
            @Index(name = "idx_listing_locality", columnList = "locality")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String addressLine;

    /** e.g. "Vijayanagar 3rd Stage" - matches a LocalityPage slug once published. */
    @Column(nullable = false)
    private String locality;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false)
    private BigDecimal rentAmount;

    private Integer bedrooms;

    private Integer bathrooms;

    /** Field-agent verification, the core trust differentiator - see risk 2 in the strategy doc. */
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    /** Bumped by the weekly WhatsApp "still available?" ping - mitigates listing rot (risk 1). */
    private Instant lastConfirmedAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
