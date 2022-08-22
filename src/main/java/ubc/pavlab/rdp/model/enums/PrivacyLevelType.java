package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PrivacyLevelType {
    PRIVATE( "Private" ),
    SHARED( "Shared" ),
    PUBLIC( "Public" );

    private final String label;
}
