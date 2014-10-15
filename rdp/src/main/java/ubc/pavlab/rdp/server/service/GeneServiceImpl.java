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
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;

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

    @Autowired
    TaxonService taxonService;

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
    public Gene findByOfficialSymbolAndTaxon( String officialSymbol, Long taxonId ) {
        return geneDao.findByOfficialSymbolAndTaxon( officialSymbol, taxonId );
    }

    @Override
    public Collection<Gene> findByTaxonId( Long id ) {
        return geneDao.findByTaxonId( id );
    }

    @Override
    public Gene findById( Long id ) {
        return geneDao.findById( id );
    }

    @Override
    public Gene load( long geneId ) {
        return geneDao.load( geneId );
    }

    /**
     * Returns a mapping of Gene objects from the json to tier type.
     * 
     * @param genesJSON - an array of JSON representations of Genes
     * @return
     */
    @Override
    public HashMap<Gene, TierType> deserializeGenes( String[] genesJSON ) throws IllegalArgumentException {
        HashMap<Gene, TierType> results = new HashMap<Gene, TierType>();
        for ( int i = 0; i < genesJSON.length; i++ ) {
            JSONObject json = new JSONObject( genesJSON[i] );
            if ( !json.has( "id" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }
            Long id = json.getLong( "id" );

            if ( id.equals( 0L ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }
            TierType tier = TierType.UNKNOWN;
            if ( json.has( "tier" ) ) {
                try {
                    tier = TierType.valueOf( json.getString( "tier" ) );
                } catch ( IllegalArgumentException e ) {
                    log.warn( "Invalid tier: (" + json.getString( "tier" ) + ") for gene: " + id );
                }
            } else {
                log.warn( "Missing tier for gene: " + id );
            }

            Gene geneFound = this.findById( id );
            if ( !( geneFound == null ) ) {
                results.put( geneFound, tier );
            } else {
                // it doesn't exist in database
                log.warn( "Cannot deserialize gene: " + id );
            }
        }

        return results;
    }

    @Override
    @Transactional
    public void updateGeneTable( String filePath ) {
        geneDao.updateGeneTable( filePath );
    }

    @Override
    @Transactional
    public void truncateGeneTable() {
        geneDao.truncateGeneTable();
    }

    @Override
    public JSONArray toJSON( Collection<GeneAssociation> geneAssociations ) {
        Collection<JSONObject> genesValuesJson = new HashSet<JSONObject>();

        for ( GeneAssociation ga : geneAssociations ) {
            JSONObject geneValuesJson = ga.toJSON();
            geneValuesJson.put( "taxonCommonName", taxonService.findById( ga.getGene().getTaxonId() ).getCommonName() );
            genesValuesJson.add( geneValuesJson );
        }

        return new JSONArray( genesValuesJson );
    }
}
