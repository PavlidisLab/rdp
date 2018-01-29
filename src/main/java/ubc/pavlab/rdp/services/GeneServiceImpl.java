package ubc.pavlab.rdp.services;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.Criteria;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.util.SearchableEhcache;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("geneService")
public class GeneServiceImpl extends SearchableEhcache<Integer, Gene> implements GeneService {

    private static Log log = LogFactory.getLog( GeneServiceImpl.class );

    private static final String CACHE_NAME = "gene";

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private EhCacheCacheManager cacheManager;

    private Ehcache cache;

    private Attribute<Integer> id;
    private Attribute<String> name;
    private Attribute<String> symbol;
    private Attribute<Integer> taxonId;
    private Attribute<String> aliases;
    private Attribute<Integer> modificationDate;

    @PostConstruct
    private void initialize() {
        this.cache = this.cacheManager.getCacheManager().getEhcache(CACHE_NAME );
        id = new Attribute<> ("id");
        name = new Attribute<> ("name");
        symbol = new Attribute<> ("symbol");
        taxonId = new Attribute<> ("taxonId");
        aliases = new Attribute<> ("aliases");
        modificationDate = new Attribute<> ("modificationDate");
    }

    public Ehcache getCache() {
        return cache;
    }

    public Integer getKey( Gene gene ) {
        return gene.getId();
    }

    @Override
    public Gene load( Integer id ) {
        return fetchByKey( id );
    }

    @Override
    public Collection<Gene> load( Collection<Integer> ids ) {
        return fetchByKey( ids );
    }

    @Override
    public Gene findBySymbolAndTaxon( String symbol, Taxon taxon ) {
        return fetchOneByCriteria( this.taxonId.eq( taxon.getId() ).and( this.symbol.eq( symbol ) ) );
    }

    @Override
    public Collection<Gene> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon ) {
        return fetchByCriteria( this.taxonId.eq( taxon.getId() ).and( this.symbol.in( symbols ) ) );
    }

    @Override
    public LinkedHashSet<Gene> autocomplete( String query, Taxon taxon ) {
        LinkedHashSet<Gene> results = new LinkedHashSet<>();
        Criteria taxonCrit = this.taxonId.eq( taxon.getId() );

        results.addAll( fetchByCriteria( taxonCrit.and( symbol.ilike( query ) ) ) );
        results.addAll( fetchByCriteria( taxonCrit.and( symbol.ilike( query + "*" ) ) ) );
        results.addAll( fetchByCriteria( taxonCrit.and( name.ilike( "*" + query + "*" ) ) ) );
        results.addAll( fetchByCriteria( taxonCrit.and( aliases.ilike( "*" + query + "*" ) ) ) );

        return results;
    }

    @Override
    public int size() {
        return this.cache.getSize();
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
    public void addAll( Collection<Gene> genes ) {
        putAll( genes );
    }

}
