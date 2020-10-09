package ubc.pavlab.rdp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.Cacheable;
import java.util.Map;

@Data
@AllArgsConstructor
public class Stats {
    private Long users;
    private Integer usersWithGenes;
    private Integer userGenes;
    private Integer uniqueUserGenes;
    private Integer uniqueUserGenesTAll; // Unique genes added, counting all TIERs
    private Integer uniqueUserGenesHumanTAll; // Unique genes mapped back to human, counting all TIERs
    private Map<String, Integer> researchersByTaxa;
}
