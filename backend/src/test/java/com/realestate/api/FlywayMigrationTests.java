package com.realestate.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The test database is built only by the Flyway migrations, and Hibernate runs with
 * ddl-auto: validate, so the context starting at all proves the migrations match the
 * entities. This also checks every migration was applied and none is left pending.
 */
@SpringBootTest
class FlywayMigrationTests {

    @Autowired private Flyway flyway;

    @Test
    void everyMigrationRanSuccessfully() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(applied).extracting(m -> m.getVersion().getVersion()).contains("1");
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isFailed()).isFalse());
        assertThat(flyway.info().pending()).isEmpty();
    }
}
