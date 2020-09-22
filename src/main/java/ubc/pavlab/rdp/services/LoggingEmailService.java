package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

/**
 * Email service implementation used for development that simply pastes the email content into the logs.
 */
@CommonsLog
@Profile("dev")
@Service("emailService")
public class LoggingEmailService implements EmailService {

    @Autowired
    private SiteSettings siteSettings;

    @Override
    public void sendSupportMessage( String message, String name, User user, HttpServletRequest request, MultipartFile attachment ) {
        log.info( MessageFormat.format( "Support message for {0}:\n{1}", user, message ) );
    }

    @Override
    public void sendResetTokenMessage( String token, User user ) {
        String url = siteSettings.getFullUrl() + "updatePassword?id=" + user.getId() + "&token=" + token;
        log.info( MessageFormat.format( "Reset URL for {0}: {1}", user, url ) );
    }

    @Override
    public void sendRegistrationMessage( User user, String token ) {
        String confirmationUrl = siteSettings.getFullUrl() + "registrationConfirm?token=" + token;
        log.info( MessageFormat.format( "Confirmation URL for {0}: {1}", user, confirmationUrl ) );
    }

    @Override
    public void sendUserRegisteredEmail( User user ) {
        log.info( MessageFormat.format( "{0} has been registered.", user ) );
    }
}
