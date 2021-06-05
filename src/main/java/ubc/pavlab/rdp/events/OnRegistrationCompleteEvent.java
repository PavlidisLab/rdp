package ubc.pavlab.rdp.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

/**
 * Created by mjacobson on 22/01/18.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class OnRegistrationCompleteEvent extends ApplicationEvent {

    private final User user;

    private final VerificationToken token;

    public OnRegistrationCompleteEvent( User user, VerificationToken token ) {
        super( user );
        this.user = user;
        this.token = token;
    }
}
