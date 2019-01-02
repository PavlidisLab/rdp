package ubc.pavlab.rdp.model;

import lombok.*;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;

@SuppressWarnings("unused")
// The object itself is not used, but needs to be present so the database is setup properly
// Also might be useful in the future
@Entity
@Table(name = "ortholog")
@Immutable
@Cacheable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "sourceGene", "targetGene", "targetTaxon" })
@IdClass(HomoKey.class)
@ToString(of = { "sourceGene", "targetGene" })
public class Ortholog {

    @Id
    private Integer sourceGene;

    @Id
    private Integer targetGene;

    @Id
    private Integer targetTaxon;

}

@EqualsAndHashCode(of = { "sourceGene", "targetGene", "targetTaxon" })
class HomoKey implements Serializable {
    private Integer sourceGene;
    private Integer targetGene;
    private Integer targetTaxon;
}
