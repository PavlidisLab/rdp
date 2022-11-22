package ubc.pavlab.rdp;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for auditing entities creation and modification with Spring Data JPA.
 *
 * @author poirigui
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
