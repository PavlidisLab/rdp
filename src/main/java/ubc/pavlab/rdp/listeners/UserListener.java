package ubc.pavlab.rdp.listeners;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import ubc.pavlab.rdp.events.OnContactEmailUpdateEvent;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.events.OnRequestAccessEvent;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.mail.MessagingException;
import java.text.MessageFormat;

@CommonsLog
@Component
public class UserListener {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @TransactionalEventListener
    public void onRegistrationComplete( OnRegistrationCompleteEvent event ) {
        if ( applicationSettings.isSendEmailOnRegistration() ) {
            try {
                emailService.sendUserRegisteredEmail( event.getUser() );
            } catch ( MessagingException e ) {
                log.error( MessageFormat.format( "Could not send registered email for {0}.", event.getUser() ), e );
            }
        }

        try {
            emailService.sendRegistrationMessage( event.getUser(), event.getToken(), event.getLocale() );
        } catch ( MessagingException e ) {
            log.error( MessageFormat.format( "Could not send registration email to {0}.", event.getUser() ), e );
        }
    }

    @TransactionalEventListener
    public void onContactEmailUpdate( OnContactEmailUpdateEvent event ) {
        try {
            emailService.sendContactEmailVerificationMessage( event.getUser(), event.getToken(), event.getLocale() );
        } catch ( MessagingException e ) {
            log.error( MessageFormat.format( "Could not send contact email verification to {0}.", event.getUser() ), e );
        }
    }

    @TransactionalEventListener
    public void onGeneRequestAccess( OnRequestAccessEvent<UserGene> event ) {
        try {
            emailService.sendUserGeneAccessRequest( event.getObject(), event.getUser(), event.getReason() );
        } catch ( MessagingException e ) {
            log.error( "Could not send access request.", e );
        }
    }
}
