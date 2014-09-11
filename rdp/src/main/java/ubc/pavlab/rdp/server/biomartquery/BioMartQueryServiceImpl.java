package ubc.pavlab.rdp.server.biomartquery;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ubc.pavlab.rdp.server.exception.BioMartServiceException;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAlias;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Simple wrapper that calls BioMart REST query service.
 * 
 * @author anton/jleong
 * @version $Id: BioMartQueryServiceImpl.java,v 1.13 2013/07/15 16:01:54 anton Exp $
 */
@Service
public class BioMartQueryServiceImpl implements BioMartQueryService {
    private static final String BIO_MART_URL_SUFFIX = "/biomart/martservice/results";
    private static final String BIO_MART_URL = "http://www.biomart.org" + BIO_MART_URL_SUFFIX;
    private static final String GENE_NAMES_BIO_MART_URL = "http://www.genenames.org" + BIO_MART_URL_SUFFIX;

    private static final Map<String, String> TAXON_COMMON_TO_DATASET = new HashMap<String, String>();
    private static final Map<String, String> TAXON_COMMON_TO_ID = new HashMap<String, String>();
    private static final Map<String, String> DATASET_NAME_TO_CHROMOSOME_FILTER = new HashMap<String, String>();

    static {
        TAXON_COMMON_TO_DATASET.put( "Human", "hsapiens_gene_ensembl" ); // 9606
        TAXON_COMMON_TO_DATASET.put( "Mouse", "mmusculus_gene_ensembl" ); // 10090
        TAXON_COMMON_TO_DATASET.put( "Rat", "rnorvegicus_gene_ensembl" ); // 10116
        // TAXON_COMMON_TO_DATASET.put("Zebrafish","drerio_gene_ensembl"); //7955
        // TAXON_COMMON_TO_DATASET.put("Fruitfly","dmelanogaster_gene_ensembl"); //7227
        // TAXON_COMMON_TO_DATASET.put("Worm","celegans_gene_ensembl"); //6239
        TAXON_COMMON_TO_DATASET.put( "Yeast", "scerevisiae_gene_ensembl" ); // 4932
        // TAXON_COMMON_TO_DATASET.put("E-coli",""); //562

        // TAXON_COMMON_TO_ID.put( "Human", "9606" );
        // TAXON_COMMON_TO_ID.put( "Mouse", "10090" );
        // TAXON_COMMON_TO_ID.put( "Rat", "10116" );
        TAXON_COMMON_TO_ID.put( "Yeast", "559292" );
        // TAXON_COMMON_TO_DATASET.put("Zebrafish","7955");
        // TAXON_COMMON_TO_DATASET.put("Fruitfly","7227");
        // TAXON_COMMON_TO_DATASET.put("Worm","6239");
        // TAXON_COMMON_TO_DATASET.put("E-coli","562");

        DATASET_NAME_TO_CHROMOSOME_FILTER.put( "hsapiens_gene_ensembl",
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,X,Y" );
        DATASET_NAME_TO_CHROMOSOME_FILTER.put( "mmusculus_gene_ensembl",
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,X,Y" );
        DATASET_NAME_TO_CHROMOSOME_FILTER.put( "rnorvegicus_gene_ensembl",
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,X" );
        // DATASET_NAME_TO_CHROMOSOME_FILTER.put( "drerio_gene_ensembl",
        // "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25" );
        // DATASET_NAME_TO_CHROMOSOME_FILTER.put( "dmelanogaster_gene_ensembl", "");
        // DATASET_NAME_TO_CHROMOSOME_FILTER.put( "celegans_gene_ensembl", "I,II,III,IV,V,X");
        DATASET_NAME_TO_CHROMOSOME_FILTER.put( "scerevisiae_gene_ensembl",
                "I,II,III,IV,V,VI,VII,VIII,IX,X,XI,XII,XIII,XIV,XV,XVI" );
        // DATASET_NAME_TO_CHROMOSOME_FILTER.put("E-coli","");

    }

    private static Log log = LogFactory.getLog( BioMartQueryServiceImpl.class.getName() );

    private static String sendRequest( String xmlQueryString, String url ) throws BioMartServiceException {

        MultivaluedMap<String, String> queryData = new MultivaluedMapImpl();
        queryData.add( "query", xmlQueryString );

        return sendRequest( queryData, url );
    }

    private static String sendRequest( MultivaluedMap<String, String> queryData, String url )
            throws BioMartServiceException {
        Client client = Client.create();

        WebResource resource = client.resource( url ).queryParams( queryData );

        ClientResponse response = resource.type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                .get( ClientResponse.class );

        // Check return code
        if ( Response.Status.fromStatusCode( response.getStatus() ).getFamily() != Response.Status.Family.SUCCESSFUL ) {
            String errorMessage = "Error occurred when accessing BioMart web service: "
                    + response.getEntity( String.class );
            log.error( errorMessage );

            throw new BioMartServiceException( errorMessage );
        }

        return response.getEntity( String.class );
    }

    @Autowired
    private BioMartCache bioMartCache;

    @Override
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols ) throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.fetchGenesByGeneSymbols( geneSymbols );
    }

    @Override
    public Collection<Gene> fetchGenesByLocation( String chromosomeName, Long start, Long end )
            throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.fetchGenesByLocation( chromosomeName, start, end );
    }

    /*
     * @Override public Collection<GenomicRange> fetchGenomicRangesByGeneSymbols( Collection<String> geneSymbols )
     * throws BioMartServiceException { Collection<Gene> genes = fetchGenesByGeneSymbols( geneSymbols );
     * Collection<GenomicRange> genomicRanges = new HashSet<GenomicRange>( genes.size() );
     * 
     * for ( Gene gene : genes ) { genomicRanges.add( gene.getGenomicRange() ); }
     * 
     * return genomicRanges; }
     */

    @Override
    public Collection<Gene> findGenes( String queryString, String taxon ) throws BioMartServiceException {
        // updateCacheIfExpired(taxon);
        updateCacheAllTaxons();

        return bioMartCache.findGenes( queryString, taxon );
    }

    /**
     * get the genes using the list of gene ids or list of gene symbols
     * 
     * @param List of gene strings
     * @return Gene value Objects associated with the given gene string list
     */
    @Override
    public List<Gene> getGenes( List<String> geneStrings ) throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.getGenes( geneStrings );
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() throws BioMartServiceException {
        // updateCacheIfExpired();
    }

    private String queryDataset( final Dataset dataset, String url ) throws BioMartServiceException {
        Query query = new Query();
        query.Dataset = dataset;

        StringWriter xmlQueryWriter = null;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance( Query.class, Dataset.class, Filter.class,
                    Attribute.class );
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            xmlQueryWriter = new StringWriter();
            jaxbMarshaller.marshal( query, xmlQueryWriter );
        } catch ( JAXBException e ) {
            String errorMessage = "Cannot initialize genes from BioMart";
            log.error( errorMessage, e );

            throw new BioMartServiceException( errorMessage );
        }

        final StopWatch timer = new StopWatch();
        timer.start();

        Timer uploadCheckerTimer = new Timer( true );
        uploadCheckerTimer.scheduleAtFixedRate( new TimerTask() {
            @Override
            public void run() {
                log.info( "Waiting for BioMart response for (" + dataset.name + ")... " + timer.getTime() + " ms" );
            }
        }, 0, 10 * 1000 );

        String response = sendRequest( xmlQueryWriter.toString(), url );
        log.info( "BioMart request to (" + url + ") for (" + dataset.name + ") took " + timer.getTime() + " ms" );

        uploadCheckerTimer.cancel();
        return response;
    }

    private Collection<Gene> queryNCBI( String taxon ) throws BioMartServiceException {

        String taxonID = TAXON_COMMON_TO_ID.get( taxon );

        MultivaluedMap<String, String> queryData = new MultivaluedMapImpl();
        queryData.add( "db", "gene" );
        queryData.add( "term", taxonID + "[Taxonomy ID] AND alive[property]" );
        queryData.add( "retmode", "json" );
        queryData.add( "usehistory", "y" );

        String response = sendRequest( queryData, "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi" );

        JSONObject json = new JSONObject( response );

        String webenv = json.getJSONObject( "esearchresult" ).getString( "webenv" );
        String querykey = json.getJSONObject( "esearchresult" ).getString( "querykey" );
        int count = json.getJSONObject( "esearchresult" ).getInt( "count" );

        log.info( response );
        log.info( "Total genes to parse: " + count );

        Collection<Gene> genes = new HashSet<>();

        for ( int retstart = 0; retstart < count; retstart += 10000 ) {

            queryData = new MultivaluedMapImpl();
            queryData.add( "db", "gene" );
            queryData.add( "query_key", querykey );
            queryData.add( "WebEnv", webenv );
            // queryData.add( "retmode", "json" );
            queryData.add( "retstart", Integer.toString( retstart ) );
            queryData.add( "retmax", "10000" );

            response = sendRequest( queryData, "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi" );

            try {
                Document xmldoc = loadXMLFromString( response );
                response = ""; // Big string, clear some memory
                NodeList nodes = xmldoc.getElementsByTagName( "DocSum" );

                for ( int i = 0; i < nodes.getLength(); i++ ) {
                    Element element = ( Element ) nodes.item( i );

                    Gene gene = new Gene();
                    gene.setTaxon( taxon );

                    Element ID = ( Element ) element.getElementsByTagName( "Id" ).item( 0 );
                    gene.setNcbiGeneId( getCharacterDataFromElement( ID ) );
                    gene.setEnsemblId( getCharacterDataFromElement( ID ) );

                    NodeList items = element.getElementsByTagName( "Item" );
                    for ( int j = 0; j < items.getLength(); j++ ) {
                        Element item = ( Element ) items.item( j );

                        String name = item.getAttribute( "Name" );

                        switch ( name ) {
                            case "Name":
                                gene.setOfficialSymbol( item.getTextContent() );
                                break;
                            case "Description":
                                gene.setOfficialName( name );
                                break;
                            case "OtherAliases":
                                break;
                        }

                        // Short circuit for-loop if all relevant info has been collected
                        if ( ( gene.getOfficialName() != null && !gene.getOfficialName().isEmpty() )
                                && ( gene.getOfficialSymbol() != null && !gene.getOfficialSymbol().isEmpty() ) ) {
                            break;
                        }

                    }

                    genes.add( gene );

                }
            } catch ( Exception e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // log.info( response );

            // FIXME replace with something that will check to see when ratelimiting is necessary
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException ex ) {
                Thread.currentThread().interrupt();
            }

            log.info( "Genes loaded so far: " + genes.size() );
        }

        return genes;
    }

    private static Document loadXMLFromString( String xml ) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource( new StringReader( xml ) );
        return builder.parse( is );
    }

    private static String getCharacterDataFromElement( Element e ) {
        Node child = e.getFirstChild();
        if ( child instanceof CharacterData ) {
            CharacterData cd = ( CharacterData ) child;
            return cd.getData();
        }
        return "?";
    }

    private Collection<Gene> parseGeneInfo( Dataset dataset, String taxon ) throws BioMartServiceException {

        dataset.Attribute.add( new Attribute( "ensembl_gene_id" ) );
        dataset.Attribute.add( new Attribute( "entrezgene" ) );
        dataset.Attribute.add( new Attribute( "external_gene_id" ) );
        dataset.Attribute.add( new Attribute( "description" ) );
        dataset.Attribute.add( new Attribute( "gene_biotype" ) );
        dataset.Attribute.add( new Attribute( "chromosome_name" ) );
        dataset.Attribute.add( new Attribute( "start" ) );
        dataset.Attribute.add( new Attribute( "end" ) );

        String response = queryDataset( dataset, BIO_MART_URL );

        String[] rows = StringUtils.split( response, "\n" );

        log.info( dataset.name + " returned " + rows.length + " rows" );

        Collection<Gene> genes = new HashSet<>();

        int rowsLength = rows.length;
        if ( rowsLength <= 1 ) {
            String errorMessage = "Error: retrieved only " + rowsLength + " row of gene data from BioMart"
                    + ( rowsLength == 1 ? "(Error message from BioMart: " + rows[0] + ")" : "" );
            log.error( errorMessage );

            throw new BioMartServiceException( errorMessage );
        }

        for ( String row : rows ) {

            // warning: split() trims off trailing whitespaces!
            String[] fields = row.split( "\t" );

            int index = 0;
            String ensemblId = fields[index++];
            String ncbiGeneId = fields[index++];
            String symbol = fields[index++];
            String name = fields[index++];
            String geneBiotype = fields[index++];
            String chromosome = fields[index++];
            String start = fields[index++];
            String end = fields[index++];

            // Ignore results that do not have required attributes.
            if ( ensemblId.equals( "" ) || chromosome.equals( "" ) || start.equals( "" ) || end.equals( "" ) ) {
                continue;
            }

            int sourceIndex = name.indexOf( " [Source:" );
            name = sourceIndex >= 0 ? name.substring( 0, sourceIndex ) : name;

            // GeneValueObject gene = new GeneValueObject( ensemblId, symbol, name, geneBiotype, taxon );
            Gene gene = new Gene();
            gene.setEnsemblId( ensemblId );
            gene.setOfficialSymbol( symbol );
            gene.setOfficialName( name );
            gene.setTaxon( taxon );
            /*
             * int startBase = Integer.valueOf( start ); int endBase = Integer.valueOf( end ); if ( startBase < endBase
             * ) { gene.setGenomicRange( new GenomicRange( chromosome, startBase, endBase ) ); } else {
             * gene.setGenomicRange( new GenomicRange( chromosome, endBase, startBase ) ); }
             */
            gene.setNcbiGeneId( ncbiGeneId );
            genes.add( gene );
        }

        return genes;
    }

    private HashMap<String, Gene> convertToMap( Collection<Gene> genes ) {
        HashMap<String, Gene> map = new HashMap<>();
        for ( Gene g : genes ) {
            map.put( g.getEnsemblId(), g );
        }
        return map;
    }

    /**
     * Queries Human gene synonyms from the HGNC Mart and updates the parameter genes
     * 
     * @param genes
     * @throws BioMartServiceException
     */
    private void addHumanGeneSynonyms( Collection<Gene> genes ) throws BioMartServiceException {
        Dataset dataset = new Dataset( "hgnc" );

        dataset.Filter
                .add( new Filter( "gd_pub_chr", DATASET_NAME_TO_CHROMOSOME_FILTER.get( "hsapiens_gene_ensembl" ) ) );

        dataset.Attribute.add( new Attribute( "gd_aliases" ) );
        dataset.Attribute.add( new Attribute( "md_ensembl_id" ) );

        String response = queryDataset( dataset, GENE_NAMES_BIO_MART_URL );

        String[] rows = StringUtils.split( response, "\n" );

        HashMap<String, Gene> genesMap = convertToMap( genes );

        for ( String row : rows ) {

            // warning: split() trims off trailing whitespaces!
            String[] fields = row.split( "\t" );

            try {
                int index = 0;
                String aliases = fields[index++];
                String ensemblId = fields[index++];
                // Ignore results that do not have required attributes.
                if ( ensemblId.equals( "" ) || aliases.equals( "" ) || !genesMap.containsKey( ensemblId ) ) {
                    continue;
                }

                for ( String alias : aliases.split( "," ) ) {
                    genesMap.get( ensemblId ).getAliases().add( new GeneAlias( alias.trim() ) );
                }

            } catch ( ArrayIndexOutOfBoundsException e ) {
                continue;
            }
        }

    }

    private void updateCacheIfExpired() throws BioMartServiceException {
        updateCacheIfExpired( "Human", false );

    }

    private void updateCacheIfExpired( String taxon, Boolean forceUpdate ) throws BioMartServiceException {
        if ( !this.bioMartCache.hasExpired() && !forceUpdate ) return;

        String datasetName = TAXON_COMMON_TO_DATASET.get( taxon );

        if ( datasetName == null ) {
            String errorMessage = "Taxon: [" + taxon + "] not recognized";
            log.error( errorMessage );

            throw new BioMartServiceException( errorMessage );
        }

        Dataset dataset = new Dataset( datasetName );

        dataset.Filter.add( new Filter( "chromosome_name", DATASET_NAME_TO_CHROMOSOME_FILTER.get( datasetName ) ) );

        Collection<Gene> genes = new HashSet<>();
        genes.addAll( parseGeneInfo( dataset, taxon ) );

        // get synonyms (only available for Human)
        if ( taxon.equals( "Human" ) ) {
            addHumanGeneSynonyms( genes );
        }

        this.bioMartCache.putAll( genes );

    }

    private void updateCacheAllTaxons() throws BioMartServiceException {
        if ( !this.bioMartCache.hasExpired() ) return;

        Collection<Gene> genes = new HashSet<>();

        for ( Map.Entry<String, String> taxon : TAXON_COMMON_TO_ID.entrySet() ) {
            // updateCacheIfExpired( taxon.getKey(), true );
            genes.addAll( queryNCBI( taxon.getKey() ) );
        }
        log.info( "Caching a total of " + genes.size() + " genes" );
        this.bioMartCache.putAll( genes );
    }

    @Override
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols, String taxon )
            throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.fetchGenesByGeneSymbols( geneSymbols, taxon );
    }

}
