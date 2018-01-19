package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneAnnotation;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.repositories.GeneAnnotationRepository;
import ubc.pavlab.rdp.repositories.GeneAnnotationRepository.AggregateCount;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("geneAnnotationService")
public class GeneAnnotationServiceImpl implements GeneAnnotationService {

    @Autowired
    GeneAnnotationRepository geneAnnotationRepository;

    @Transactional
    @Override
    public GeneAnnotation create( GeneAnnotation geneAnnotation ) {
        return geneAnnotationRepository.save( geneAnnotation );
    }

    @Transactional
    @Override
    public void delete( GeneAnnotation geneAnnotation ) {
        geneAnnotationRepository.delete( geneAnnotation );
    }

    @Override
    public Collection<GeneAnnotation> findByGeneOntologyId( String geneOntologyId ) {
        return geneAnnotationRepository.findByPkGoId( geneOntologyId );
    }

    @Override
    public Collection<GeneAnnotation> findByGeneOntologyIdAndTaxon( String geneOntologyId, Taxon taxon ) {
        return geneAnnotationRepository.findByPkGoIdAndPkGeneTaxon( geneOntologyId, taxon );
    }

    @Override
    public Collection<GeneAnnotation> findByGene( Gene gene ) {
        return geneAnnotationRepository.findByPkGene( gene );
    }

    @Override
    public GeneAnnotation findByGeneAndGeneOntologyId( Gene gene, String geneOntologyId ) {
        return geneAnnotationRepository.findByPkGoIdAndPkGene( geneOntologyId, gene );
    }

    @Override
    public Integer countGenesForGeneOntologyId( String geneOntologyId ) {
        return geneAnnotationRepository.countByPkGoId( geneOntologyId );
    }

    @Override
    public Integer countGenesForGeneOntologyIdAndTaxon( String geneOntologyId, Taxon taxon ) {
        return geneAnnotationRepository.countByPkGoIdAndPkGeneTaxon( geneOntologyId, taxon );
    }

    @Override
    public Collection<GeneAnnotation> findByGeneLimitedByTermSize( Gene gene, int limit ) {
        Collection<GeneAnnotation> genesAnnotations = findByGene( gene );
        for ( Iterator<GeneAnnotation> i = genesAnnotations.iterator(); i.hasNext(); ) {
            GeneAnnotation ga = i.next();
            // TODO: This might be slow...
            Integer count = countGenesForGeneOntologyId( ga.getPk().getGoId() );
            if ( count > limit ) {
                i.remove();
            }
        }

        return genesAnnotations;

    }

    @Override
    public Collection<GeneAnnotation> loadAll() {
        return geneAnnotationRepository.findAll();
    }

    @Transactional
    @Override
    public void updateGeneAnnotationTable( String filePath ) {
        // TODO: Make me
    }

    @Transactional
    @Override
        public void truncateGeneAnnotationTable() {
        // TODO: Make me
    }

    @Override
    public List<AggregateCount> calculateDirectSizes() {
        return geneAnnotationRepository.calculateDirectSizes();
    }

    @Override
    public Collection<Gene> annotationToGene( Collection<GeneAnnotation> geneAnnotations ) {
        Collection<Gene> results = new HashSet<Gene>();

        for ( GeneAnnotation ga : geneAnnotations ) {
            results.add( ga.getGene() );
        }

        return results;
    }

}
