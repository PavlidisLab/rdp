package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Getter
@Setter
@Table(name = "user_organ",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "uberon_id" }) })
public class UserOrgan extends Organ implements UserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @ManyToOne
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
    public Optional<User> getOwner() {
        return Optional.of( user );
    }

    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return user.getEffectivePrivacyLevel();
    }
}
