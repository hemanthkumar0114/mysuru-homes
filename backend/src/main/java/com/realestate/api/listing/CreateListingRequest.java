package com.realestate.api.listing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateListingRequest(
        @NotNull(message = "is required") ListingType type,
        @NotBlank @Size(max = 150, message = "must be 150 characters or fewer") String title,
        @NotBlank @Size(max = 255, message = "must be 255 characters or fewer") String addressLine,
        @NotBlank @Size(max = 100, message = "must be 100 characters or fewer") String locality,
        @NotNull(message = "is required")
                @DecimalMin(value = "-90.0", message = "must be between -90 and 90")
                @DecimalMax(value = "90.0", message = "must be between -90 and 90")
                Double lat,
        @NotNull(message = "is required")
                @DecimalMin(value = "-180.0", message = "must be between -180 and 180")
                @DecimalMax(value = "180.0", message = "must be between -180 and 180")
                Double lng,
        @NotNull(message = "is required")
                @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0")
                @DecimalMax(value = "10000000", message = "must be 10,000,000 or less")
                @Digits(integer = 8, fraction = 2, message = "can have at most 2 decimal places")
                BigDecimal rentAmount,
        @Min(value = 0, message = "must be between 0 and 20") @Max(value = 20, message = "must be between 0 and 20")
                Integer bedrooms,
        @Min(value = 0, message = "must be between 0 and 20") @Max(value = 20, message = "must be between 0 and 20")
                Integer bathrooms) {}
