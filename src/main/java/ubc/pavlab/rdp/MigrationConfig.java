package ubc.pavlab.rdp;

import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Configuration related to the database migration, generally involving {@link org.flywaydb.core.Flyway}.
 *
 * @author poirigui
 */
@Configuration
@CommonsLog
public class MigrationConfig {

    @RequiredArgsConstructor
    private enum NewChecksumReason {
        /**
         * A migration applied with Flyway 3.
         * <p>
         * The checksum algorithm has changed.
         */
        FLYWAY_3( "has been performed with Flyway 3.2.1" ),
        /**
         * A migration that introduced a regression with MySQL 5.7.
         */
        MYSQL_5_7_REGRESSION( "introduced a regression with MySQL 5.7" );
        final String reason;
    }

    /**
     * Expected migration information that are used to determine if a repair is necessary.
     */
    @RequiredArgsConstructor
    private static class ExpectedMigrationInfo {
        private final String version;
        private final int preRepairChecksum;
        private final int postRepairChecksum;
        private final NewChecksumReason newChecksumReason;
    }

    private static final ExpectedMigrationInfo[] MIGRATION_META = {
            new ExpectedMigrationInfo( "1.0.0", -330642568, 1889522940, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.3.2", 1109324745, 1109324745, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.0", 310023814, 1017485172, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.1", -1543189200, 1330781885, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.2", 1706447069, 253128706, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.3", 571489108, 571489108, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.6", 399726006, 504755998, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.11", 1536441374, 709491229, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.4.11.1", -1312864724, -907949910, NewChecksumReason.FLYWAY_3 ),
            new ExpectedMigrationInfo( "1.5.0.4", 625408518, 1560455114, NewChecksumReason.MYSQL_5_7_REGRESSION )
    };

    /**
     * Remove the 'version_rank' column and perform a {@link org.flywaydb.core.Flyway#repair()} to fix migration
     * checksums.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                // drop version_rank column
                Connection connection = flyway.getConfiguration().getDataSource().getConnection();
                // lookup the schema_version table and version_rank column
                boolean hasVersionRankColumn;
                try ( ResultSet resultSet = connection.getMetaData().getColumns( null, null, "schema_version", "version_rank" ) ) {
                    hasVersionRankColumn = resultSet.next();
                }
                if ( hasVersionRankColumn ) {
                    log.warn( "The 'schema_version' table is still using the 'version_rank' column from Flyway 3.2.1; will proceed to remove it..." );
                    connection.createStatement().execute( "alter table schema_version drop column version_rank" );
                }
            } catch ( SQLException e ) {
                throw new RuntimeException( e );
            }
            MigrationInfo[] appliedMigrations = flyway.info().applied();
            boolean repairNeeded = false;
            for ( MigrationInfo appliedMigration : appliedMigrations ) {
                ExpectedMigrationInfo expectedMigration = Arrays.stream( MIGRATION_META )
                        .filter( mm -> mm.version.equals( appliedMigration.getVersion().getVersion() ) )
                        .findAny()
                        .orElse( null );
                if ( expectedMigration != null &&
                        appliedMigration.getChecksum() != null &&
                        appliedMigration.getChecksum().equals( expectedMigration.preRepairChecksum ) &&
                        !appliedMigration.getChecksum().equals( expectedMigration.postRepairChecksum ) ) {
                    repairNeeded = true;
                    log.warn( String.format( "Flyway migration %s %s, checksum will be bumped from %s to %s after repair.",
                            appliedMigration.getVersion(), expectedMigration.newChecksumReason.reason,
                            appliedMigration.getChecksum(), expectedMigration.postRepairChecksum ) );
                }
            }
            if ( repairNeeded ) {
                log.warn( "Flyway repair will now be performed." );
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
