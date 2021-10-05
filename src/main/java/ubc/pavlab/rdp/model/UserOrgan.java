package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.util.Optional;

/**
 * Organ systems tracked by a user.
 * <p>
 * TODO: add user to {@link EqualsAndHashCode} definition to distinguish
 * between organs from different users.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = { "user" }, callSuper = true)
@Table(name = "user_organ",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "uberon_id" }) })
public class UserOrgan extends Organ implements UserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    public static UserOrgan createFromOrganInfo( User user, OrganInfo organInfo ) {
        UserOrgan userOrgan = new UserOrgan();
        userOrgan.setUser( user );
        userOrgan.setUberonId( organInfo.getUberonId() );
        userOrgan.setName( organInfo.getName() );
        userOrgan.setDescription( organInfo.getDescription() );
        return userOrgan;
    }

    @Override
    @JsonIgnore
    public Optional<User> getOwner() {
        return Optional.of( user );
    }

    @Override
    @JsonIgnore
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return user.getEffectivePrivacyLevel();
    }

    public void updateOrgan( OrganInfo organInfo ) {
        setUberonId( organInfo.getUberonId() );
        setName( organInfo.getName() );
        setDescription( organInfo.getDescription() );
    }
}
