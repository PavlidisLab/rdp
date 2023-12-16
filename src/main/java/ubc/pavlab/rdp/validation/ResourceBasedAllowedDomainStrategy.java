package ubc.pavlab.rdp.validation;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A resource-based strategy for allowing domains.
 * <p>
 * The resource is read line-by-line, each line being a domain that will be allowed. The domains validated using a
 * {@link SetBasedAllowedDomainStrategy}, so all of its rules regarding ASCII characters and case-insensitivity applies.
 * <p>
 * The strategy accepts an optional refresh delay which it will use to determine if the resource should be reloaded. If
 * the resource is backed by a {@link java.io.File}, the last modified date will also be used to prevent unnecessary
 * reload.
 * <p>
 * If the refresh fails for any reason, an error is logged and the previous list of allowed domains is used until
 * another refresh is attempted. If no previous list of allowed domains exist, the exception will be raised. For this
 * reason, you might want to invoke {@link #refresh()} right after creating the strategy to catch any error early otn.
 *
 * @author poirigui
 */
@CommonsLog
public class ResourceBasedAllowedDomainStrategy implements AllowedDomainStrategy {

    /**
     * Resolution to use when comparing the last modified of a file against a recorded timestamp with
     * {@link System#currentTimeMillis()}.
     */
    private final static int LAST_MODIFIED_RESOLUTION_MS = 10;

    /**
     * A resource where email domains are found.
     */
    private final Resource allowedEmailDomainsFile;

    /**
     * A refresh delay, in ms.
     */

    private final Duration refreshDelay;

    /* internal state */
    private volatile SetBasedAllowedDomainStrategy strategy;
    private long lastRefresh;

    public ResourceBasedAllowedDomainStrategy( Resource allowedEmailDomainsFile, Duration refreshDelay ) {
        this.allowedEmailDomainsFile = allowedEmailDomainsFile;
        this.refreshDelay = refreshDelay;
    }

    @Override
    public boolean allows( String domain ) {
        if ( strategy == null || shouldRefresh() ) {
            try {
                refresh();
            } catch ( Exception e ) {
                if ( strategy == null ) {
                    throw new RuntimeException( e );
                } else {
                    // pretend the resource has been refreshed, otherwise it will be reattempted on every request
                    this.lastRefresh = System.currentTimeMillis();
                    log.error( String.format( "An error occurred while refreshing the list of allowed domains from %s. The previous list will be used until the next refresh.", allowedEmailDomainsFile ), e );
                }
            }
        }
        return strategy.allows( domain );
    }

    /**
     * Refresh the list of allowed domains.
     *
     * @throws IOException if an error occurred while reading the resource.
     */
    public synchronized void refresh() throws IOException {
        StopWatch timer = StopWatch.createStarted();
        Set<String> allowedDomains;
        try ( BufferedReader ir = new BufferedReader( new InputStreamReader( allowedEmailDomainsFile.getInputStream() ) ) ) {
            allowedDomains = new HashSet<>();
            String line;
            int lineno = 0;
            while ( ( line = ir.readLine() ) != null ) {
                lineno++;
                if ( StringUtils.isAsciiPrintable( line ) ) {
                    allowedDomains.add( line.trim() );
                } else {
                    log.warn( String.format( "Invalid characters in line %d from %s, it will be ignored.", lineno, allowedEmailDomainsFile ) );
                }
            }
        }
        strategy = new SetBasedAllowedDomainStrategy( allowedDomains );
        lastRefresh = System.currentTimeMillis();
        log.info( String.format( "Loaded %d domains from %s in %d ms.", allowedDomains.size(), allowedEmailDomainsFile, timer.getTime() ) );
    }

    /**
     * Obtain a set of allowed email domains.
     */
    public Set<String> getAllowedDomains() {
        if ( strategy == null ) {
            return Collections.emptySet();
        } else {
            return strategy.getAllowedDomains();
        }
    }

    /**
     * Verify if the resource should be reloaded.
     */
    private boolean shouldRefresh() {
        if ( refreshDelay == null ) {
            return false;
        }
        if ( System.currentTimeMillis() - lastRefresh >= refreshDelay.toMillis() ) {
            // check if the file is stale
            try {
                long lastModified = FileUtils.lastModified( allowedEmailDomainsFile.getFile() );
                return lastModified + LAST_MODIFIED_RESOLUTION_MS > lastRefresh;
            } catch ( FileNotFoundException ignored ) {
                //  resource is not backed by a file, most likely
            } catch ( IOException e ) {
                log.error( String.format( "An error occurred while checking the last modified date of %s.", allowedEmailDomainsFile ), e );
            }
            return true;
        }
        return false;
    }
}
