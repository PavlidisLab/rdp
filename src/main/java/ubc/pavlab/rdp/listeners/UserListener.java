package ubc.pavlab.rdp.listeners;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.events.OnContactEmailUpdateEvent;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
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

    @EventListener
    public void onRegistrationComplete( OnRegistrationCompleteEvent event ) {
        if ( applicationSettings.isSendEmailOnRegistration() ) {
            try {
                emailService.sendUserRegisteredEmail( event.getUser() );
            } catch ( MessagingException e ) {
                log.error( MessageFormat.format( "Could not send registered email for {0}.", event.getUser() ), e );
            }
        }

        try {
            emailService.sendRegistrationMessage( event.getUser(), event.getToken() );
        } catch ( MessagingException e ) {
            log.error( MessageFormat.format( "Could not send registration email to {0}.", event.getUser() ), e );
        }
    }

    @EventListener
    public void onContactEmailUpdate( OnContactEmailUpdateEvent event ) {
        try {
            emailService.sendContactEmailVerificationMessage( event.getUser(), event.getToken() );
        } catch ( MessagingException e ) {
            log.error( MessageFormat.format( "Could not send contact email verification to {0}.", event.getUser() ), e );
        }
    }
}
