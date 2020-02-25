package ubc.pavlab.rdp.model;

import lombok.NonNull;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import java.util.Optional;

/**
 * Indicate that the content is privacy sensitive.
 */
public interface PrivacySensitive {

    /**
     * Obtain the owner of the content if applicable.
     */
    public Optional<User> getOwner();

    /**
     * Obtain the effective privacy level of the content.
     *
     * In many cases, content have intrinsic privacy level that might be undefined; the implementation has to perform
     * cascading generally by using the owner privacy level.
     */
    @NonNull
    public PrivacyLevelType getEffectivePrivacyLevel();
}
