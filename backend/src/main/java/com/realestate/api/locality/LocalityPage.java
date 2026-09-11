package com.realestate.api.locality;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Auto-generated locality landing page - the cheapest acquisition channel per the GTM doc. */
@Entity
@Table(name = "locality_pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalityPage {

    @Id
    private String slug;

    @Column(nullable = false)
    private String localityName;

    @Lob
    private String seoContent;

    private BigDecimal avgRent;
}
