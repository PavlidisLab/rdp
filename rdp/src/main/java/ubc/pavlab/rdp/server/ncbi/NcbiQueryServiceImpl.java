/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.ncbi;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

import ubc.pavlab.rdp.server.exception.NcbiServiceException;
import ubc.pavlab.rdp.server.model.Gene;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Calls NCBI E-Utils SOAP query service.
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service
public class NcbiQueryServiceImpl implements NcbiQueryService {
    private static final String NCBI_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    private static final String NCBI_SEARCH = NCBI_URL + "esearch.fcgi";
    private static final String NCBI_SUMMARY = NCBI_URL + "esummary.fcgi";

    private static final Map<String, String> TAXON_COMMON_TO_ID = new HashMap<String, String>();

    private static AtomicBoolean updatingCache = new AtomicBoolean( false );

    static {
        // TAXON_COMMON_TO_ID.put( "Human", "9606" );
        // TAXON_COMMON_TO_ID.put( "Mouse", "10090" );
        // TAXON_COMMON_TO_ID.put( "Rat", "10116" );
        TAXON_COMMON_TO_ID.put( "Yeast", "559292" );
        // TAXON_COMMON_TO_ID.put( "Zebrafish", "7955" );
        // TAXON_COMMON_TO_ID.put( "Fruitfly", "7227" );
        // TAXON_COMMON_TO_ID.put( "Worm", "6239" );
        // TAXON_COMMON_TO_ID.put( "E. Coli", "562" );
    }

    private static Log log = LogFactory.getLog( NcbiQueryServiceImpl.class.getName() );

    @Autowired
    private NcbiCache ncbiCache;

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.ncbi.NcbiQueryService#fetchGenesByGeneSymbols(java.util.Collection, java.lang.String)
     */
    @Override
    public Collection<Gene> fetchGenesByGeneSymbolsAndTaxon( Collection<String> geneSymbols, String taxon )
            throws NcbiServiceException {

        return ncbiCache.fetchGenesByGeneSymbolsAndTaxon( geneSymbols, taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.ncbi.NcbiQueryService#fetchGenesByGeneSymbols(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols ) throws NcbiServiceException {

        return ncbiCache.fetchGenesByGeneSymbols( geneSymbols );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.ncbi.NcbiQueryService#findGenes(java.lang.String, java.lang.String)
     */
    @Override
    public Collection<Gene> findGenes( String queryString, String taxon ) throws NcbiServiceException {

        return ncbiCache.findGenes( queryString, taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.ncbi.NcbiQueryService#getGenes(java.util.List)
     */
    @Override
    public List<Gene> getGenes( List<String> geneStrings ) throws NcbiServiceException {

        return ncbiCache.getGenes( geneStrings );
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() throws NcbiServiceException {
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

    public void clearCache() throws NcbiServiceException {
        this.ncbiCache.clearAll();
    }

    public int updateCache() throws NcbiServiceException {
        int cacheSize = -1;
        if ( updatingCache.compareAndSet( false, true ) ) {
            try {
                Collection<Gene> genes = new HashSet<>();
                for ( Map.Entry<String, String> taxon : TAXON_COMMON_TO_ID.entrySet() ) {
                    genes.addAll( queryNCBI( taxon.getKey() ) );
                }
                log.info( "Caching a total of " + genes.size() + " genes" );
                this.ncbiCache.putAll( genes );
                cacheSize = this.ncbiCache.size();
                log.info( "Current size of Cache: " + cacheSize );
            } finally {
                updatingCache.set( false );
            }
        } else {
            String errorMessage = "Update Cache already running!";
            log.error( errorMessage );
            throw new IllegalThreadStateException( errorMessage );
        }

        return cacheSize;
    }

    private Collection<Gene> queryNCBI( final String taxon ) throws NcbiServiceException {

        String taxonID = TAXON_COMMON_TO_ID.get( taxon );

        final StopWatch timer = new StopWatch();
        timer.start();

        MultivaluedMap<String, String> queryData = new MultivaluedMapImpl();
        queryData.add( "db", "gene" );
        queryData.add( "term", taxonID + "[Taxonomy ID] AND alive[property]" );
        queryData.add( "retmode", "json" );
        queryData.add( "usehistory", "y" );

        String response = sendRequest( queryData, NCBI_SEARCH, taxon );

        JSONObject json = new JSONObject( response );

        String webenv = json.getJSONObject( "esearchresult" ).getString( "webenv" );
        String querykey = json.getJSONObject( "esearchresult" ).getString( "querykey" );
        int count = json.getJSONObject( "esearchresult" ).getInt( "count" );

        // log.info( response );
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

            response = sendRequest( queryData, NCBI_SUMMARY, taxon );

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
                    // gene.setEnsemblId( getCharacterDataFromElement( ID ) );

                    NodeList items = element.getElementsByTagName( "Item" );
                    int conditionBits = 0;
                    for ( int j = 0; j < items.getLength(); j++ ) {
                        Element item = ( Element ) items.item( j );

                        String name = item.getAttribute( "Name" );

                        switch ( name ) {
                            case "Name":
                                gene.setOfficialSymbol( item.getTextContent() );
                                conditionBits = conditionBits | ( 1 << 0 ); // Set first bit to 1
                                break;
                            case "Description":
                                gene.setOfficialName( item.getTextContent() );
                                conditionBits = conditionBits | ( 1 << 1 ); // Set second bit to 1
                                break;
                            case "OtherAliases":
                                gene.parseAliases( item.getTextContent() );
                                conditionBits = conditionBits | ( 1 << 2 );
                                break;
                        }

                        // Short circuit for-loop if all relevant info has been collected,
                        // integer to check against is ( 2^N - 1 ) where N is the # of cases
                        if ( conditionBits == 7 ) {
                            break;
                        }

                    }

                    genes.add( gene );

                }
            } catch ( Exception e ) {
                timer.stop();
                String errorMessage = "Error parsing response from NCBI";
                log.error( errorMessage, e );

                throw new NcbiServiceException( errorMessage );
            }

            // log.info( response );

            // FIXME replace with something that will check to see when ratelimiting is necessary
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException ex ) {
                Thread.currentThread().interrupt();
            }

            log.info( "Genes loaded so far: " + genes.size() + " / " + count );
        }

        log.info( "NCBI request for (" + taxon + ") took " + timer.getTime() + " ms" );

        timer.stop();
        return genes;
    }

    private static String sendRequest( MultivaluedMap<String, String> queryData, final String url,
            final String logDescription ) throws NcbiServiceException {
        Client client = Client.create();

        log.info( "Sending request to (" + url + ") for " + logDescription + "..." );

        final StopWatch timer = new StopWatch();
        timer.start();

        Timer responseCheckerTimer = new Timer( true );
        responseCheckerTimer.scheduleAtFixedRate( new TimerTask() {
            @Override
            public void run() {
                log.info( "Waiting for NCBI response from (" + url + ") for " + logDescription + "... "
                        + timer.getTime() + " ms" );
            }
        }, 10 * 1000, 10 * 1000 );

        ClientResponse response = null;

        try {
            WebResource resource = client.resource( url ).queryParams( queryData );

            response = resource.type( MediaType.APPLICATION_FORM_URLENCODED_TYPE ).get( ClientResponse.class );

            // Check return code
            if ( Response.Status.fromStatusCode( response.getStatus() ).getFamily() != Response.Status.Family.SUCCESSFUL ) {
                String errorMessage = "Failed Response when accessing NCBI web service: "
                        + response.getEntity( String.class );
                log.error( errorMessage );

                throw new NcbiServiceException( errorMessage );
            }

        } catch ( Exception e ) {
            responseCheckerTimer.cancel();
            String errorMessage = "Error sending request to NCBI";
            log.error( errorMessage, e );

            throw new NcbiServiceException( errorMessage );
        }

        String r = response.getEntity( String.class );

        responseCheckerTimer.cancel();
        timer.stop();
        log.info( "Response received from (" + url + ") for " + logDescription + "." );

        return r;
    }

}
