package ubc.pavlab.rdp.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Stats {
    /**
     * Version of this registry.
     */
    private String version;
    /**
     * Number of users.
     */
    private Long users;
    /**
     * NUmber of public users.
     */
    private Long publicUsers;
    /**
     * Number of users with genes.
     */
    private Long usersWithGenes;
    /**
     * NUmber of genes.
     */
    private Long userGenes;
    /**
     * Unique TIER 1 and 2 genes.
     */
    private Long uniqueUserGenes;
    /**
     * Unique genes in all tiers.
     */
    private Long uniqueUserGenesInAllTiers;
    /**
     * Unique human genes
     */
    private Long uniqueHumanUserGenesInAllTiers;
    /**
     * Number of researchers per taxon.
     */
    private Map<Integer, Long> researchersByTaxonId;
}
