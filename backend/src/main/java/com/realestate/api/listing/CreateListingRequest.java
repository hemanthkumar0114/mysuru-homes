package com.realestate.api.listing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateListingRequest(
        @NotNull(message = "is required") ListingType type,
        @NotBlank String title,
        @NotBlank String addressLine,
        @NotBlank String locality,
        @NotNull(message = "is required") Double lat,
        @NotNull(message = "is required") Double lng,
        @NotNull(message = "is required") @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0") BigDecimal rentAmount,
        Integer bedrooms,
        Integer bathrooms) {}
