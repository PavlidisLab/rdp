package ubc.pavlab.rdp.validation;

/**
 * Defines a strategy to determine if a domain is allowed.
 *
 * @author poirigui
 */
@FunctionalInterface
public interface AllowedDomainStrategy {

    boolean allows( String domain );
}
