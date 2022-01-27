package ubc.pavlab.rdp.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.util.Locale;

@Getter
@EqualsAndHashCode(callSuper = false)
public class OnContactEmailUpdateEvent extends ApplicationEvent {

    private final User user;

    private final VerificationToken token;

    private final Locale locale;

    public OnContactEmailUpdateEvent( User user, VerificationToken token, Locale locale ) {
        super( user );
        this.user = user;
        this.token = token;
        this.locale = locale;
    }
}
