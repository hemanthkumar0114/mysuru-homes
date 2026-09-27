package com.realestate.api.locality;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the six Mysuru-corridor locality pages if the locality_pages table is empty.
 * It only ever adds to an empty table, so edits you make to these rows later are never
 * overwritten. The text is starter content - rewrite it in the database whenever you like.
 * typicalRent is an indicative monthly figure, not a market study.
 */
@Component
@RequiredArgsConstructor
public class LocalitySeeder implements CommandLineRunner {

    private final LocalityPageRepository repository;

    @Value("${app.seed-localities:true}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled || repository.count() > 0) {
            return;
        }
        repository.saveAll(
                List.of(
                        page(
                                "vijayanagar",
                                "Vijayanagar",
                                "A well-established residential layout in Mysuru, built in numbered stages. Wide roads, parks and neighbourhood markets make it popular with families and working professionals. Rentals and PGs here are in steady demand.",
                                "12000"),
                        page(
                                "hebbal",
                                "Hebbal",
                                "Home to the Hebbal Industrial Area and several large employers, including the Infosys Mysuru campus. Young professionals look here for rentals and PGs within a short commute of work.",
                                "13000"),
                        page(
                                "hootagalli",
                                "Hootagalli",
                                "A fast-growing area on the edge of Mysuru with an industrial estate and newer housing. Rents are generally lower than in the city centre, which suits those working nearby.",
                                "10000"),
                        page(
                                "bogadi",
                                "Bogadi",
                                "A residential area along Bogadi Road with a mix of independent houses and newer layouts. Good road links and everyday shops close by.",
                                "9500"),
                        page(
                                "dattagalli",
                                "Dattagalli",
                                "A developing residential neighbourhood with newer layouts and a mix of independent houses and apartments. Typically one of the more affordable places to rent in the corridor.",
                                "9000"),
                        page(
                                "ramakrishnanagar",
                                "Ramakrishnanagar",
                                "A settled residential neighbourhood with schools, clinics and markets nearby. A calm setting for families who want an established area.",
                                "14000")));
    }

    private static LocalityPage page(String slug, String name, String description, String typicalRent) {
        return LocalityPage.builder()
                .slug(slug)
                .localityName(name)
                .seoContent(description)
                .avgRent(new BigDecimal(typicalRent))
                .build();
    }
}
