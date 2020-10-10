package ubc.pavlab.rdp.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

@Getter
public class OnContactEmailUpdateEvent extends ApplicationEvent {

    private User user;

    private VerificationToken token;

    public OnContactEmailUpdateEvent( User user, VerificationToken token ) {
        super( user );
        this.user = user;
        this.token = token;
    }
}
