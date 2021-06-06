package ubc.pavlab.rdp.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Stats {
    private Long users;
    private Long publicUsers;
    private Integer usersWithGenes;
    private Integer userGenes;
    private Integer uniqueUserGenes;
    private Integer uniqueUserGenesTAll; // Unique genes added, counting all TIERs
    private Integer uniqueUserGenesHumanTAll; // Unique genes mapped back to human, counting all TIERs
    private Map<String, Integer> researchersByTaxa;
}
