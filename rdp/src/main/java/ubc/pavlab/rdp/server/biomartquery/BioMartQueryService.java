package ubc.pavlab.rdp.server.biomartquery;

import java.util.Collection;
import java.util.List;

import ubc.pavlab.rdp.server.exception.BioMartServiceException;
import ubc.pavlab.rdp.server.model.GeneValueObject;
import ubc.pavlab.rdp.server.model.GenomicRange;

/**
 * Used to request gene and other data using BioMart.
 * 
 * @author frances/jleong
 * @version $Id: BioMartQueryService.java,v 1.15 2013/06/11 22:30:47 anton Exp $
 */
public interface BioMartQueryService {

    /**
     * Find genes by gene symbols.
     * 
     * @param geneSymbols
     * @return collection of genes
     * @throws BioMartServiceException
     */
    public Collection<GeneValueObject> fetchGenesByGeneSymbols( Collection<String> geneSymbols )
            throws BioMartServiceException;

    /**
     * Find genes that are inside the specified region of the genome.
     * 
     * @param chromosomeName - 1,2,3,X, etc
     * @param start -
     * @param end -
     * @return collection of genes
     * @throws BioMartServiceException
     */
    public Collection<GeneValueObject> fetchGenesByLocation( String chromosomeName, Long start, Long end )
            throws BioMartServiceException;

    /**
     * Find genomic ranges by gene symbols.
     * 
     * @param geneSymbols
     * @return collection of genomic ranges
     * @throws BioMartServiceException
     */
    public Collection<GenomicRange> fetchGenomicRangesByGeneSymbols( Collection<String> geneSymbols )
            throws BioMartServiceException;

    public Collection<GeneValueObject> findGenes( String queryString ) throws BioMartServiceException;

    /**
     * Get a list of genes using the given gene symbols or ensembl ids. The order of the returned list of genes is
     * preserved. If a gene symbol or ensembl id is not valid, the returned gene will be null.
     * 
     * @param geneStrings gene symbols or ensembl ids
     * @return a list of GeneValueObjects
     * @throws BioMartServiceException
     */
    public List<GeneValueObject> getGenes( List<String> geneStrings ) throws BioMartServiceException;
}