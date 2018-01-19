package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.UserGene.TierType;
import ubc.pavlab.rdp.repositories.GeneRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("geneService")
public class GeneServiceImpl implements GeneService {

    private static Log log = LogFactory.getLog( GeneServiceImpl.class );

    @Autowired
    private GeneRepository geneRepository;

    @Transactional
    @Override
    public Gene create( Gene gene ) {
        return geneRepository.save(gene);
    }

    @Transactional
    @Override
    public void update( Gene gene ) {
        geneRepository.save(gene);

    }

    @Override
    public Gene load( Integer id ) {
        return geneRepository.findOne( id );
    }

    @Override
    public Collection<Gene> load( Collection<Integer> ids ) {
        return geneRepository.findByIdIn(ids);
    }

    @Transactional
    @Override
    public void delete( Gene gene ) {
        geneRepository.delete( gene );
    }

    @Override
    public Collection<Gene> loadAll() {
        return geneRepository.findAll();
    }

    @Override
    public Collection<Gene> findByOfficialSymbol( String officialSymbol ) {
        return geneRepository.findBySymbol( officialSymbol );
    }

    @Override
    public Gene findByOfficialSymbolAndTaxon( String officialSymbol, Taxon taxon ) {
        return geneRepository.findBySymbolAndTaxon(officialSymbol, taxon);
    }

    @Override
    public Collection<Gene> findByTaxonId( Integer id ) {
        return geneRepository.findByTaxonId(id);
    }

    @Override
    public Collection<Gene> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon ) {
        return geneRepository.findBySymbolInAndTaxon(symbols, taxon);
    }

    @Override
    public List<Gene> autocomplete( String query, Taxon taxon ) {
        return geneRepository.autocomplete( query, taxon );
    }

    @Override
    public HashMap<Gene, TierType> deserializeGenes( String[] genesJSON ) {
        HashMap<Gene, TierType> results = new HashMap<Gene, TierType>();
        for ( String aGenesJSON : genesJSON ) {
            JSONObject json = new JSONObject( aGenesJSON );
            if ( !json.has( "id" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }
            Integer id = json.getInt( "id" );

            if ( id.equals( 0L ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }

            Gene geneFound = this.load( id );

            if ( !(geneFound == null) ) {
                results.put( geneFound, extractTierFromJSON( json ) );
            } else {
                // it doesn't exist in database
                log.warn( "Cannot deserialize gene: " + id );
            }
        }

        return results;
    }

    @Override
    public HashMap<Gene, TierType> quickDeserializeGenes( String[] genesJSON ) throws IllegalArgumentException {
        HashMap<Gene, TierType> results = new HashMap<>();
        HashMap<Integer, TierType> ids = new HashMap<>();
        for ( String aGenesJSON : genesJSON ) {
            JSONObject json = new JSONObject( aGenesJSON );
            if ( !json.has( "id" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }
            Integer id = json.getInt( "id" );

            if ( id.equals( 0L ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }

            ids.put( id, extractTierFromJSON( json ) );
        }

        if ( ids.size() > 0 ) {
            Collection<Gene> genes = load( ids.keySet() );

            for ( Integer id : ids.keySet() ) {
                boolean found = false;

                for ( Gene g : genes ) {
                    if ( id.equals( g.getId() ) ) {
                        found = true;
                        results.put( g, ids.get( id ) );
                        break;
                    }
                }

                if ( !found ) {
                    log.warn( "Cannot deserialize gene: " + id );
                }
            }
        }

        return results;
    }

    private TierType extractTierFromJSON(JSONObject json) {
        TierType tier = TierType.UNKNOWN;
        if ( json.has( "tier" ) ) {
            try {
                tier = TierType.valueOf( json.getString( "tier" ) );
            } catch ( IllegalArgumentException e ) {
                log.warn( "Invalid tier: (" + json.getString( "tier" ) + ") for gene: " + json.getInt( "id" ) );
            }
        } else {
            log.warn( "Missing tier for gene: " + json.getInt( "id" ) );
        }
        return tier;
    }

    @Override
    public void updateGeneTable( String filePath ) {
        //TODO: MAKE ME
    }

    @Override
    public void truncateGeneTable() {
        //TODO: MAKE ME
    }

    @Override
    public JSONArray toJSON( Collection<UserGene> geneAssociations ) {
        Collection<JSONObject> genesValuesJson = new HashSet<JSONObject>();

        for ( UserGene ga : geneAssociations ) {
            JSONObject geneValuesJson = ga.toJSON();
            geneValuesJson.put( "taxonCommonName", ga.getGene().getTaxon().getCommonName() );
            genesValuesJson.add( geneValuesJson );
        }

        return new JSONArray( genesValuesJson );
    }
}
