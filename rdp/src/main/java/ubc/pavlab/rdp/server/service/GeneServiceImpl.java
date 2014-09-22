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

package ubc.pavlab.rdp.server.service;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.model.Gene;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Service("geneService")
public class GeneServiceImpl implements GeneService {

    private static Log log = LogFactory.getLog( ResearcherServiceImpl.class );

    @Autowired
    GeneDao geneDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#create(ubc.pavlab.rdp.server.model.Gene)
     */
    @Override
    public Gene create( Gene gene ) {
        return geneDao.create( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#update(ubc.pavlab.rdp.server.model.Gene)
     */
    @Override
    public void update( Gene gene ) {
        geneDao.update( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#delete(ubc.pavlab.rdp.server.model.Gene)
     */
    @Override
    public void delete( Gene gene ) {
        geneDao.remove( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#loadAll()
     */
    @Override
    public Collection<Gene> loadAll() {
        return ( Collection<Gene> ) geneDao.loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneService#findByOfficalSymbol(java.lang.String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbol( String officialSymbol ) {
        return geneDao.findByOfficialSymbol( officialSymbol );
    }

    @Override
    public Gene findByOfficialSymbol( String officialSymbol, String taxon ) {
        return geneDao.findByOfficialSymbolAndTaxon( officialSymbol, taxon );
    }

    @Override
    public Gene load( long geneId ) {
        return geneDao.load( geneId );
    }

    /**
     * Returns a collection of Gene objects from the json.
     * 
     * @param genesJSON - an array of JSON representations of Genes
     * @return
     */
    @Override
    public Collection<Gene> deserializeGenes( String[] genesJSON ) throws IllegalArgumentException {
        Collection<Gene> results = new HashSet<>();
        for ( int i = 0; i < genesJSON.length; i++ ) {
            JSONObject json = new JSONObject( genesJSON[i] );
            if ( !json.has( "officialSymbol" ) || !json.has( "taxon" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned symbol and organism." );
            }
            String symbol = json.getString( "officialSymbol" );
            String taxon = json.getString( "taxon" );
            if ( symbol.equals( "" ) || taxon.equals( "" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned symbol and organism." );
            }
            Gene geneFound = this.findByOfficialSymbol( symbol, taxon );
            if ( !( geneFound == null ) ) {
                results.add( geneFound );
            } else {
                // it doesn't exist yet
                Gene gene = new Gene();
                gene.parseJSON( genesJSON[i] );
                log.info( "Creating new gene: " + gene.toString() );
                results.add( this.create( gene ) );
            }
        }

        return results;
    }

}
