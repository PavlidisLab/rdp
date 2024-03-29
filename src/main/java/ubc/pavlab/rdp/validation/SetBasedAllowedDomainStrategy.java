package ubc.pavlab.rdp.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple strategy for allowing domain based on a case-insensitive set.
 * <p>
 * The supplied set can only contain domain with ASCII-printable characters. If you want to allow <a>IDN</a>, store
 * Punycode in the set and enable IDN in {@link EmailValidator#EmailValidator(Set, boolean)}.
 */
public class SetBasedAllowedDomainStrategy implements AllowedDomainStrategy {

    private final Set<String> allowedDomains;

    public SetBasedAllowedDomainStrategy( Collection<String> allowedDomains ) {
        // ascii-only domains, case-insensitive
        Assert.isTrue( allowedDomains.stream().allMatch( StringUtils::isAsciiPrintable ),
                "Allowed domains must only contain ASCII-printable characters." );
        this.allowedDomains = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
        this.allowedDomains.addAll( allowedDomains );
    }

    @Override
    public boolean allows( String domain ) {
        return allowedDomains.contains( domain );
    }

    public Set<String> getAllowedDomains() {
        return Collections.unmodifiableSet( allowedDomains );
    }
}
