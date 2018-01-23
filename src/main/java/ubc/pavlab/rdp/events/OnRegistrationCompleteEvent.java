package ubc.pavlab.rdp.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;

/**
 * Created by mjacobson on 22/01/18.
 */
@Getter
@Setter
public class OnRegistrationCompleteEvent extends ApplicationEvent {

    private User user;

    public OnRegistrationCompleteEvent( User user ) {
        super( user );
        this.user = user;

    }
}
