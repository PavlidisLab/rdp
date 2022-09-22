package ubc.pavlab.rdp.util;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuilderInitializer;

/**
 * Register custom SQL functions so that they can be mentioned in HQL.
 * <p>
 * Note: this requires a registration under <a href="classpath:META-INF/services/org.hibernate.boot.spi.MetadataBuilderInitializer">classpath:META-INF/services/org.hibernate.boot.spi.MetadataBuilderInitializer</a>
 *
 * @author poirigui
 */
public class CustomMetadataBuilderInitializer implements MetadataBuilderInitializer {
    @Override
    public void contribute( MetadataBuilder metadataBuilder, StandardServiceRegistry standardServiceRegistry ) {
        metadataBuilder.applySqlFunction( "match", new MatchAgainstSQLFunction() );
    }
}
