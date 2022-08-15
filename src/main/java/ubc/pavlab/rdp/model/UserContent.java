package ubc.pavlab.rdp.model;

import lombok.NonNull;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * Indicate content that belongs to a user.
 */
public interface UserContent {

    /**
     * Obtain the owner of the content if applicable.
     */
    Optional<User> getOwner();

    /**
     * Obtain the effective privacy level of the content.
     * <p>
     * In many cases, content have intrinsic privacy level that might be undefined; the implementation has to perform
     * cascading generally by using the owner privacy level.
     */
    @NonNull
    PrivacyLevelType getEffectivePrivacyLevel();

    /**
     * Exact moment when this user-associated content was created, or null if unknown.
     */
    Timestamp getCreatedAt();
}
