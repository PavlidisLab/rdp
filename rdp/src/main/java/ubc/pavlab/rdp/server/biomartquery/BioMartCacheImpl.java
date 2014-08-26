package ubc.pavlab.rdp.server.biomartquery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.Criteria;

import org.springframework.stereotype.Component;

import ubc.pavlab.rdp.server.model.GeneValueObject;
import ubc.pavlab.rdp.server.util.SearchableEhcache;

/**
 * BioMart cache implementation
 * 
 * @author jleong
 * @version $Id$
 */
@Component
public class BioMartCacheImpl extends SearchableEhcache<GeneValueObject> implements BioMartCache {
    // These constants are used in ehcache.xml. If they are changed, ehcache.xml must be modified.
    private static final String CACHE_NAME = "BioMartCache";
    private static final String GENE_ENSEMBL_ID_SEARCH_ATTRIBUTE_NAME = "ensemblId";
    private static final String GENE_NAME_SEARCH_ATTRIBUTE_NAME = "officialName";
    private static final String GENE_SYMBOL_SEARCH_ATTRIBUTE_NAME = "officialSymbol";
    private static final String CHROMOSOME_SEARCH_ATTRIBUTE_NAME = "genomicRangeChromosome";
    private static final String START_SEARCH_ATTRIBUTE_NAME = "genomicRangeStart";
    private static final String END_SEARCH_ATTRIBUTE_NAME = "genomicRangeEnd";
    private static final String TAXON_SEARCH_ATTRIBUTE_NAME = "taxon";

    private Attribute<Object> geneEnsemblIdAttribute;
    private Attribute<Object> geneNameAttribute;
    private Attribute<Object> geneSymbolAttribute;
    private Attribute<Object> chromosomeAttribute;
    private Attribute<Object> startAttribute;
    private Attribute<Object> endAttribute;
    private Attribute<Object> taxonAttribute;

    @Override
    public Collection<GeneValueObject> fetchGenesByGeneSymbols( Collection<String> geneSymbols ) {
        Criteria symbolCriteria = geneSymbolAttribute.in( geneSymbols );

        return fetchByCriteria( symbolCriteria );
    }

    @Override
    public Collection<GeneValueObject> fetchGenesByGeneTaxon( Collection<String> taxons ) {
        Criteria taxonCriteria = taxonAttribute.in( taxons );

        return fetchByCriteria( taxonCriteria );
    }
    
    @Override
    public Collection<GeneValueObject> fetchGenesByLocation( String chromosomeName, Long start, Long end ) {
        Criteria chromosomeCriteria = chromosomeAttribute.eq( chromosomeName );
        Criteria insideVariant = startAttribute.between( start.intValue(), end.intValue() ).or(
                endAttribute.between( start.intValue(), end.intValue() ) );
        Criteria overlapsStart = startAttribute.le( start.intValue() ).and( endAttribute.ge( start.intValue() ) );
        Criteria overlapsEnd = startAttribute.le( end.intValue() ).and( endAttribute.ge( end.intValue() ) );

        Criteria hasName = geneSymbolAttribute.ne( "" );

        final Collection<GeneValueObject> geneValueObjects = fetchByCriteria( hasName.and( chromosomeCriteria
                .and( insideVariant.or( overlapsStart ).or( overlapsEnd ) ) ) );

        return geneValueObjects;
    }

    @Override
    public Collection<GeneValueObject> findGenes( String queryString ) {
        String regexQueryString = "*" + queryString + "*";

        Criteria nameCriteria = geneNameAttribute.ilike( regexQueryString );
        Criteria symbolCriteria = geneSymbolAttribute.ilike( regexQueryString );

        return fetchByCriteria( nameCriteria.or( symbolCriteria ) );
    }

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public List<GeneValueObject> getGenes( List<String> geneStrings ) {
        List<GeneValueObject> genes = new ArrayList<GeneValueObject>( geneStrings.size() );

        for ( String geneString : geneStrings ) {
            Criteria symbolCriteria = geneSymbolAttribute.ilike( geneString );
            Criteria ensemblIdCriteria = geneEnsemblIdAttribute.ilike( geneString );
            Collection<GeneValueObject> fetchedGenes = fetchByCriteria( symbolCriteria.or( ensemblIdCriteria ) );
            if ( fetchedGenes.size() > 0 ) {
                // Only use the first gene.
                genes.add( fetchedGenes.iterator().next() );
            } else {
                genes.add( null );
            }
        }

        return genes;
    }

    @Override
    public Object getKey( GeneValueObject gene ) {
        return gene.getEnsemblId();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
        geneEnsemblIdAttribute = getSearchAttribute( GENE_ENSEMBL_ID_SEARCH_ATTRIBUTE_NAME );
        geneNameAttribute = getSearchAttribute( GENE_NAME_SEARCH_ATTRIBUTE_NAME );
        geneSymbolAttribute = getSearchAttribute( GENE_SYMBOL_SEARCH_ATTRIBUTE_NAME );
        chromosomeAttribute = getSearchAttribute( CHROMOSOME_SEARCH_ATTRIBUTE_NAME );
        startAttribute = getSearchAttribute( START_SEARCH_ATTRIBUTE_NAME );
        endAttribute = getSearchAttribute( END_SEARCH_ATTRIBUTE_NAME );
        taxonAttribute = getSearchAttribute( TAXON_SEARCH_ATTRIBUTE_NAME );
    }

}
