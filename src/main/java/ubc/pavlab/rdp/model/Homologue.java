package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "homologue")
@Immutable
@Cacheable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"sourceGene", "targetGene", "targetTaxon"})
@IdClass(HomoKey.class)
@ToString(of = {"sourceGene", "targetGene"})
public class Homologue {

    @Id
    private Integer sourceGene;

    @Id
    private Integer targetGene;

    @Id
    private Integer targetTaxon;

}

@EqualsAndHashCode(of = {"sourceGene", "targetGene", "targetTaxon"})
class HomoKey implements Serializable {
    private Integer sourceGene;
    private Integer targetGene;
    private Integer targetTaxon;
}
