package ubc.pavlab.rdp.security;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.Token;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Challenge the token by checking the User-Agent header.
 *
 * @author poirigui
 */
@Component
public class UserAgentChallenge implements SecureTokenChallenge<HttpServletRequest> {

    private final static Set<String> ACCEPTED_AGENT_CLASSES = new HashSet<>( Arrays.asList( "Browser", "Browser Webview" ) );

    private final UserAgentAnalyzer userAgentAnalyzer;

    public UserAgentChallenge() {
        this.userAgentAnalyzer = UserAgentAnalyzer
                .newBuilder()
                .withField( "AgentClass" )
                .useJava8CompatibleCaching()
                .withCache( 10000 )
                .build();
    }

    @Override
    public void challenge( Token token, HttpServletRequest request ) throws TokenException {
        String userAgent = request.getHeader( "User-Agent" );
        if ( StringUtils.isBlank( userAgent ) ) {
            throw new TokenException( "The User-Agent header is missing or blank." );
        }
        // unfortunately, Yauaa is not thread safe
        UserAgent.ImmutableUserAgent ua;
        synchronized ( userAgentAnalyzer ) {
            ua = userAgentAnalyzer.parse( userAgent );
        }
        if ( !ACCEPTED_AGENT_CLASSES.contains( ua.get( "AgentClass" ).getValue() ) ) {
            throw new TokenException( "Unacceptable User-Agent header." );
        }
    }

    @Override
    public boolean supports( Class<?> clazz ) {
        return HttpServletRequest.class.isAssignableFrom( clazz );
    }
}
