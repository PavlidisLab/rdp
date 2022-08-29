package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

@AllArgsConstructor
@Getter
public enum PrivacyLevelType {
    PRIVATE( "Private" ),
    SHARED( "Shared" ),
    PUBLIC( "Public" );

    private final String label;

    public MessageSourceResolvable getResolvableTitle() {
        return new DefaultMessageSourceResolvable( "PrivacyLevelType." + name() );
    }

    @JsonValue
    public int toJsonValue() {
        return ordinal();
    }
}
