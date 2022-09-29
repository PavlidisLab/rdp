package ubc.pavlab.rdp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = { "uberonId" })
@ToString(of = { "uberonId" })
public abstract class Organ {

    @NaturalId
    @Column(name = "uberon_id", nullable = false)
    private String uberonId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Obtain a resolvable title for this organ system.
     */
    public MessageSourceResolvable resolvableTitle() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies.uberon." + name + ".title" }, null, StringUtils.capitalize( name ) );
    }

    /**
     * Obtain a resolvable definition for this organ system.
     */
    public MessageSourceResolvable resolvableDefinition() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies.uberon." + name + ".definition" }, null, description );
    }
}
