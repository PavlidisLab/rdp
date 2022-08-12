package ubc.pavlab.rdp;

import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
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

    /**
     * Expected migration information that are used to determine if a repair is necessary.
     */
    @RequiredArgsConstructor
    private static class ExpectedMigrationInfo {
        private final String version;
        private final int pre15Checksum;
        private final int post15Checksum;
    }

    private static final ExpectedMigrationInfo[] MIGRATION_META = {
            new ExpectedMigrationInfo( "1.0.0", -330642568, 1889522940 ),
            new ExpectedMigrationInfo( "1.3.2", 1109324745, 1109324745 ),
            new ExpectedMigrationInfo( "1.4.0", 310023814, 1017485172 ),
            new ExpectedMigrationInfo( "1.4.1", -1543189200, 1330781885 ),
            new ExpectedMigrationInfo( "1.4.2", 1706447069, 253128706 ),
            new ExpectedMigrationInfo( "1.4.3", 571489108, 571489108 ),
            new ExpectedMigrationInfo( "1.4.6", 399726006, 504755998 ),
            new ExpectedMigrationInfo( "1.4.11", 1536441374, 709491229 ),
            new ExpectedMigrationInfo( "1.4.11.1", -1312864724, -907949910 )
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
                boolean hasVersionRankColumn = false;
                ResultSetMetaData metadata = connection.createStatement().executeQuery( "select * from schema_version" ).getMetaData();
                for ( int i = 0; i < metadata.getColumnCount(); i++ ) {
                    if ( metadata.getColumnName( i + 1 ).equals( "version_rank" ) ) {
                        hasVersionRankColumn = true;
                    }
                }
                if ( hasVersionRankColumn ) {
                    log.info( "The 'schema_version' table is still using the 'version_rank' column from Flyway 3.2.1; will proceed to remove it." );
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
                        appliedMigration.getChecksum().equals( expectedMigration.pre15Checksum ) &&
                        !appliedMigration.getChecksum().equals( expectedMigration.post15Checksum ) ) {
                    log.warn( String.format( "Flyway migration %s has been performed with Flyway 3.2.1, checksum will be bumped from %s to %s after repair.",
                            appliedMigration.getVersion(), appliedMigration.getChecksum(), expectedMigration.post15Checksum ) );
                    repairNeeded = true;
                }
            }
            if ( repairNeeded ) {
                log.warn( "Flyway 3.2.1 migrations detected, Flyway repair will be performed." );
                flyway.repair();
            }
            flyway.migrate();
        };
    }
}
