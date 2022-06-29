package ubc.pavlab.rdp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Repository
public interface UserGeneRepository extends JpaRepository<UserGene, Integer>, JpaSpecificationExecutor<UserGene> {

    Integer countByTierIn( Collection<TierType> tiers );

    @Query("select count(distinct geneId) FROM UserGene WHERE tier IN (:tiers)")
    Integer countDistinctGeneByTierIn( @Param("tiers") Collection<TierType> tiers );

    @Query("select count(distinct user) FROM UserGene WHERE taxon = :taxon")
    Integer countDistinctUserByTaxon( @Param("taxon") Taxon taxon );

    @Query("select count(distinct user) FROM UserGene")
    Integer countDistinctUser();

    /**
     * Find all genes from enabled users.
     */
    Page<UserGene> findByUserEnabledTrue( Pageable pageable );

    /**
     * Find all user genes that fall within a given privacy level amonng enabled users.
     * <p>
     * If the user gene privacy level is less strict than the profile value or null, then the profile value is taken.
     */
    @Query("select ug from UserGene as ug join ug.user where ug.user.enabled is true and (case when ug.privacyLevel is null or ug.privacyLevel > ug.user.profile.privacyLevel then ug.user.profile.privacyLevel else ug.privacyLevel end) = :privacyLevel")
    Page<UserGene> findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( @Param("privacyLevel") PrivacyLevelType privacyLevel, Pageable pageable );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    UserGene findById( int id );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneId( int geneId );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneIdAndTier( int geneId, TierType tier );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneIdAndTierIn( int geneId, Set<TierType> tiers );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxon( String symbolContaining, Taxon taxon );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTier( String symbolContaining, Taxon taxon, TierType tier );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( String symbolContaining, Taxon taxon, Set<TierType> tiers );

    UserGene findBySymbolAndTaxon( String symbol, Taxon taxon );

    /**
     * Find all user genes ortholog to a given gene.
     */
    @Query("select user_gene from UserGene user_gene where user_gene.geneId in (select ortholog.geneId from GeneInfo gene_info join gene_info.orthologs as ortholog where gene_info.geneId = :geneId)")
    Collection<UserGene> findOrthologsByGeneId( @Param("geneId") Integer geneId );

    /**
     * Find user genes within a given taxon that are ortholog to a given gene.
     */
    @Query("select user_gene from UserGene user_gene where user_gene.geneId in (select ortholog.geneId from GeneInfo gene_info join gene_info.orthologs as ortholog where gene_info.geneId = :geneId and ortholog.taxon = :taxon)")
    Collection<UserGene> findOrthologsByGeneIdAndTaxon( @Param("geneId") Integer geneId, @Param("taxon") Taxon taxon );

    /**
     * @param taxonId a taxon identifier in which to perform orthology search from
     * @return a collection of tuple whose first element is a {@link UserGene} and second element is the corresponding
     * {@link GeneInfo} ortholog in the supplied taxon.
     */
    @Query(value = "select gi.gene_id from gene_info as gi join ortholog on gi.id = ortholog.source_gene  join gene_info as gi2 on gi2.id = ortholog.target_gene join gene ug on ug.gene_id = gi2.gene_id where gi.taxon_id = :taxonId", nativeQuery = true)
    Collection<Integer> findOrthologGeneIdsByOrthologToTaxon( @Param("taxonId") Integer taxonId );

    // Return all human genes.
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select geneId FROM UserGene where taxon = '9606'")
    Collection<Integer> findAllHumanGenes();

    @Query("select gene from UserGene gene left join fetch gene.geneInfo")
    Collection<UserGene> findAllWithGeneInfo();
}
