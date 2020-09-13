package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;

@AllArgsConstructor
@Getter
public enum PrivacyLevelType {
    PRIVATE( "Private", "Your information will only be accessible by administrators." ),
    SHARED( "Shared", "Your information will be accessible by other researchers who are registered." ),
    PUBLIC( "Public", "Your information will be visible by everyone." );

    private String label;
    private String description;

    @JsonValue
    public int toJsonValue() {
        return this.ordinal();
    }
}
