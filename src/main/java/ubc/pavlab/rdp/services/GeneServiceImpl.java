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

import java.util.*;

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
        // Yes, I know... sue me.
        return geneRepository.autocomplete( query, query, query, query, taxon );
    }



    @Override
    public Map<Gene, TierType> deserializeGenes( Map<Integer, TierType> genesTierMap) {
        Map<Gene, TierType> results = new HashMap<>();

        Collection<Gene> genes = load( genesTierMap.keySet() );

        for ( Gene gene : genes ) {
            results.put( gene, genesTierMap.get( gene.getId() ) );
        }

        return results;
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
