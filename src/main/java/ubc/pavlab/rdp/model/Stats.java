package ubc.pavlab.rdp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import javax.persistence.Cacheable;
import java.util.Map;

@Getter
@Setter
@Immutable
@Cacheable
@AllArgsConstructor
public class Stats {
    private Long users;
    private Integer usersWithGenes;
    private Integer userGenes;
    private Integer uniqueUserGenes;
    private Map<String, Integer> researchersByTaxa;
}
