package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
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
    public Future<Void> sendSupportMessage( String message, String name, User user, String userAgent, MultipartFile attachment, Locale locale ) {
        log.info( MessageFormat.format( "Support message for {0}:\n{1}", user, message ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<Void> sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) {
        String url = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "updatePassword" )
                .queryParam( "id", user.getId() )
                .queryParam( "token", token.getToken() )
                .build().encode().toUriString();
        log.info( MessageFormat.format( "Reset URL for {0}: {1}", user, url ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<Void> sendRegistrationMessage( User user, VerificationToken token, Locale locale ) {
        String confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "registrationConfirm" )
                .queryParam( "token", token.getToken() )
                .build()
                .encode()
                .toUriString();
        log.info( MessageFormat.format( "Confirmation URL for {0}: {1}", user, confirmationUrl ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<Void> sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) {
        String confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "user/verify-contact-email" )
                .queryParam( "token", token.getToken() )
                .build()
                .encode()
                .toUriString();
        log.info( MessageFormat.format( "Contact email verification URL for {0}: {1}", user, confirmationUrl ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<Void> sendUserRegisteredEmail( User user ) {
        log.info( MessageFormat.format( "{0} has been registered.", user ) );
        return CompletableFuture.completedFuture( null );
    }

    @Override
    public Future<Void> sendUserGeneAccessRequest( UserGene userGene, User by, String reason ) {
        log.info( MessageFormat.format( "{0} has been requested by {1} for: {2}.", userGene, by, reason ) );
        return CompletableFuture.completedFuture( null );
    }
}
