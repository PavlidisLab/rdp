package ubc.pavlab.rdp.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

@Getter
@EqualsAndHashCode(callSuper = true)
public class OnContactEmailUpdateEvent extends ApplicationEvent {

    private final User user;

    private final VerificationToken token;

    public OnContactEmailUpdateEvent( User user, VerificationToken token ) {
        super( user );
        this.user = user;
        this.token = token;
    }
}
