package ubc.pavlab.rdp.services;

import org.springframework.security.access.annotation.Secured;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneAnnotation;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.util.AggregateCount;

import java.util.Collection;
import java.util.List;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GeneAnnotationService {

    @Secured({ "ADMIN" })
    GeneAnnotation create( final GeneAnnotation geneAnnotation );

    @Secured({ "ADMIN" })
    void delete( GeneAnnotation geneAnnotation );

    Collection<GeneAnnotation> findByGeneOntologyId( final String geneOntologyId );

    Collection<GeneAnnotation> findByGeneOntologyIdAndTaxon( final String geneOntologyId, final Taxon taxon );

    Collection<GeneAnnotation> findByGene( final Gene gene );

    GeneAnnotation findByGeneAndGeneOntologyId( final Gene gene, final String geneOntologyId );

    Integer countGenesForGeneOntologyId( final String geneOntologyId );

    Integer countGenesForGeneOntologyIdAndTaxon( String geneOntologyId, Taxon taxon );

    Collection<GeneAnnotation> findByGeneLimitedByTermSize( final Gene gene, final int limit );

    @Secured({ "ADMIN" })
    Collection<GeneAnnotation> loadAll();

    @Secured({ "ADMIN" })
    void updateGeneAnnotationTable( String filePath );

    @Secured({ "ADMIN" })
    void truncateGeneAnnotationTable();

    Collection<Gene> annotationToGene( Collection<GeneAnnotation> geneAnnotations );

    List<AggregateCount> calculateDirectSizes();

}