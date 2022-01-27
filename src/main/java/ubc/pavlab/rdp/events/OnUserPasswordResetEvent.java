package ubc.pavlab.rdp.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;

import java.util.Locale;

@Getter
@EqualsAndHashCode(callSuper = false)
public class OnUserPasswordResetEvent extends ApplicationEvent {

    private final User user;
    private final PasswordResetToken token;
    private final Locale locale;

    public OnUserPasswordResetEvent( User user, PasswordResetToken token, Locale locale ) {
        super( user );
        this.user = user;
        this.token = token;
        this.locale = locale;
    }

}
