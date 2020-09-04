package ubc.pavlab.rdp;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class DatabaseMigrationConfig {

    @Bean
    public FlywayMigrationInitializer flywayInitializer( Flyway flyway ) {
        return new FlywayMigrationInitializer( flyway, ( f ) -> { } );
    }

    /**
     * Delays the Flyway migration past Hibernate schema creation.
     *
     * TODO: Dump the initial migration in Flyway's baseline migration.
     */
    @Bean
    @DependsOn("entityManagerFactory")
    public FlywayMigrationInitializer delayedFlywayInitializer( Flyway flyway ) {
        return new FlywayMigrationInitializer( flyway, null );
    }
}
