package ubc.pavlab.rdp.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.net.IDN;
import java.util.Set;

/**
 * Validate an email address against a list of allowed domains.
 * <p>
 * If no list of allowed domains is provided, any domain is allowed and only basic validation is performed.
 * <p>
 * If enabled, this validator can accept international domain names (IDN) and verify them against the list of allowed
 * domains by first converting them to Punycode using {@link IDN#toASCII(String)}.
 *
 * @author poirigui
 */
public class EmailValidator implements Validator {

    /**
     * List of allowed domains.
     */
    private final AllowedDomainStrategy allowedDomainStrategy;

    /**
     * Allow international domain names.
     */
    private final boolean allowIdn;

    public EmailValidator() {
        this.allowedDomainStrategy = null;
        this.allowIdn = false;
    }

    public EmailValidator( @Nullable AllowedDomainStrategy allowedDomainStrategy, boolean allowIdn ) {
        this.allowedDomainStrategy = allowedDomainStrategy;
        this.allowIdn = allowIdn;
    }

    public EmailValidator( Set<String> allowedDomains, boolean allowIdn ) {
        this( new SetBasedAllowedDomainStrategy( allowedDomains ), allowIdn );
    }

    @Override
    public boolean supports( Class<?> clazz ) {
        return String.class.isAssignableFrom( clazz );
    }

    @Override
    public void validate( Object target, Errors errors ) {
        String email = (String) target;
        String[] parts = email.split( "@", 2 );
        if ( parts.length != 2 ) {
            errors.rejectValue( null, "EmailValidator.invalidAddress" );
            return;
        }
        String address = parts[0];
        if ( address.isEmpty() ) {
            errors.rejectValue( null, "EmailValidator.emptyUser" );
        }
        String domain = parts[1];
        if ( domain.isEmpty() ) {
            errors.rejectValue( null, "EmailValidator.emptyDomain" );
            return;
        }
        if ( allowIdn ) {
            try {
                domain = IDN.toASCII( domain );
            } catch ( IllegalArgumentException e ) {
                errors.rejectValue( null, "EmailValidator.domainNotConformToRfc3490", new String[]{ e.getMessage() }, null );
                return;
            }
        } else if ( !StringUtils.isAsciiPrintable( domain ) ) {
            errors.rejectValue( null, "EmailValidator.domainContainsUnsupportedCharacters" );
            return;
        }
        if ( allowedDomainStrategy != null && !allowedDomainStrategy.allows( domain ) ) {
            // at this point, the domain only contains ascii-printable, so it can safely be passed back to the user in
            // an error message
            errors.rejectValue( null, "EmailValidator.domainNotAllowed", new String[]{ domain }, null );
        }
    }
}
