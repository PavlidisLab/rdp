package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Email service implementation used for development that simply pastes the email content into the logs.
 */
@CommonsLog
@Profile("dev")
@Service("emailService")
public class LoggingEmailServiceImpl implements EmailService {

    @Autowired
    private SiteSettings siteSettings;

    @Override
    public Future<?> sendSupportMessage( String message, String name, User user, String userAgent, @Nullable MultipartFile attachment, Locale locale ) {
        log.info( MessageFormat.format( "Support message for {0}:\n{1}", user, message ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<?> sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) {
        URI url = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "updatePassword" )
                .queryParam( "id", "{id}" )
                .queryParam( "token", "{token}" )
                .build( new HashMap<String, String>() {
                    {
                        put( "id", user.getId().toString() );
                        put( "token", token.getToken() );
                    }
                } );
        log.info( MessageFormat.format( "Reset URL for {0}: {1}", user, url ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<?> sendRegistrationMessage( User user, VerificationToken token, Locale locale ) {
        URI confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "registrationConfirm" )
                .queryParam( "token", "{token}" )
                .build( Collections.singletonMap( "token", token.getToken() ) );
        log.info( MessageFormat.format( "Confirmation URL for {0}: {1}", user, confirmationUrl ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<?> sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) {
        URI confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "user/verify-contact-email" )
                .queryParam( "token", "{token}" )
                .build( Collections.singletonMap( "token", token.getToken() ) );
        log.info( MessageFormat.format( "Contact email verification URL for {0}: {1}", user, confirmationUrl ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<?> sendUserRegisteredEmail( User user ) {
        log.info( MessageFormat.format( "{0} has been registered.", user ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<?> sendUserGeneAccessRequest( UserGene userGene, User by, String reason ) {
        log.info( MessageFormat.format( "{0} has been requested by {1} for: {2}.", userGene, by, reason ) );
        return CompletableFuture.completedFuture( null );
    }
}
