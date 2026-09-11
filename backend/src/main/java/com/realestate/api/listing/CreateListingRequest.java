package com.realestate.api.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateListingRequest(
        @NotNull ListingType type,
        @NotBlank String title,
        @NotBlank String addressLine,
        @NotBlank String locality,
        @NotNull Double lat,
        @NotNull Double lng,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal rentAmount,
        Integer bedrooms,
        Integer bathrooms) {}
