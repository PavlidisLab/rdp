package ubc.pavlab.rdp.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.User;

import java.io.Serializable;

@Getter
@EqualsAndHashCode(callSuper = false)
public class OnRequestAccessEvent<T extends Serializable> extends ApplicationEvent {

    private User user;
    private T object;
    private String reason;

    public OnRequestAccessEvent( User user, T object, String reason ) {
        super( user );
        this.user = user;
        this.object = object;
        this.reason = reason;
    }
}
