package ubc.pavlab.rdp.server.biomartquery;

import java.util.Collection;
import java.util.List;

import ubc.pavlab.rdp.server.model.GeneValueObject;

/**
 * BioMart cache
 * 
 * @author jleong
 * @version $Id$
 */
public interface BioMartCache {
    public Collection<GeneValueObject> fetchGenesByGeneSymbols( Collection<String> geneSymbols );

    public Collection<GeneValueObject> fetchGenesByLocation( String chromosomeName, Long start, Long end );

    public Collection<GeneValueObject> findGenes( String queryString );

    /**
     * Get a list of genes using the given gene symbols or ensembl ids. The order of the returned list of genes is
     * preserved. If a gene symbol or ensembl id is not valid, the returned gene will be null.
     * 
     * @param geneStrings gene symbols or ensembl ids
     * @return a list of GeneValueObjects
     */
    public List<GeneValueObject> getGenes( List<String> geneStrings );

    public boolean hasExpired();

    public void putAll( Collection<GeneValueObject> genes );
}