package ubc.pavlab.rdp.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.util.Locale;

/**
 * Created by mjacobson on 22/01/18.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class OnRegistrationCompleteEvent extends ApplicationEvent {

    private final User user;

    private final VerificationToken token;

    private final Locale locale;

    public OnRegistrationCompleteEvent( User user, VerificationToken token, Locale locale ) {
        super( user );
        this.user = user;
        this.token = token;
        this.locale = locale;
    }
}
