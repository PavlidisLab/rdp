package ubc.pavlab.rdp.util;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

/**
 * Constants for various messages to use in the code.
 *
 * @author poirigui
 */
public final class Messages {

    public static final MessageSourceResolvable SHORTNAME = new DefaultMessageSourceResolvable( "rdp.site.shortname" );
    public static final MessageSourceResolvable FULLNAME = new DefaultMessageSourceResolvable( "rdp.site.fullname" );
}
