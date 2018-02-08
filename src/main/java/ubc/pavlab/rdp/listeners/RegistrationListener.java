package ubc.pavlab.rdp.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.util.UUID;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    ApplicationSettings applicationSettings;

    @Override
    public void onApplicationEvent( OnRegistrationCompleteEvent event ) {

        if ( applicationSettings.isSendEmailOnRegistration() ) {
            emailService.sendUserRegisteredEmail( event.getUser() );
        }

        this.confirmRegistration( event );
    }

    private void confirmRegistration( OnRegistrationCompleteEvent event ) {
        User user = event.getUser();
        String token = UUID.randomUUID().toString();
        userService.createVerificationTokenForUser( user, token );
        emailService.sendRegistrationMessage( user, token );
    }
}
