package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GeneInfoParser;
import ubc.pavlab.rdp.util.GeneOrthologsParser;
import ubc.pavlab.rdp.util.SearchResult;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("geneService")
@CommonsLog
public class GeneInfoServiceImpl implements GeneInfoService {

    @Autowired
    GeneInfoRepository geneInfoRepository;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private GeneInfoParser geneInfoParser;

    @Autowired
    GeneOrthologsParser geneOrthologsParser;

    @Override
    public GeneInfo load( Integer id ) {
        return geneInfoRepository.findByGeneId( id );
    }

    @Override
    public Collection<GeneInfo> load( Collection<Integer> ids ) {
        return geneInfoRepository.findAllByGeneIdIn( ids );
    }

    @Override
    public Collection<GeneInfo> loadAll() {
        return geneInfoRepository.findAll();
    }

    @Override
    public Collection<GeneInfo> findAllByActiveTaxon() {
        return geneInfoRepository.findAllByTaxonActiveTrue();
    }

    @Override
    public GeneInfo findBySymbolAndTaxon( String symbol, Taxon taxon ) {
        return geneInfoRepository.findBySymbolAndTaxon( symbol, taxon );
    }

    @Override
    public Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon ) {
        return geneInfoRepository.findBySymbolInAndTaxon( symbols, taxon );
    }

    @Override
    public Collection<SearchResult<GeneInfo>> autocomplete( String query, Taxon taxon, int maxResults ) {
        Collection<SearchResult<GeneInfo>> results = new LinkedHashSet<>();

        if ( addAll( results, geneInfoRepository.findAllBySymbolAndTaxon( query, taxon ), GeneMatchType.EXACT_SYMBOL, maxResults ) ) {
            return results;
        }

        if ( addAll( results, geneInfoRepository.findAllBySymbolStartingWithIgnoreCaseAndTaxon( query, taxon ),
                GeneMatchType.SIMILAR_SYMBOL, maxResults ) ) {
            return results;
        }

        if ( addAll( results, geneInfoRepository.findAllByNameStartingWithIgnoreCaseAndTaxon( query, taxon ),
                GeneMatchType.SIMILAR_NAME, maxResults ) ) {
            return results;
        }

        addAll( results, geneInfoRepository.findAllByAliasesContainingIgnoreCaseAndTaxon( query, taxon ),
                GeneMatchType.SIMILAR_ALIAS, maxResults );

        return results;
    }

    @Override
    public Map<GeneInfo, TierType> deserializeGenesTiers( Map<Integer, TierType> genesTierMap ) {
        return genesTierMap.keySet().stream()
                .filter( geneId -> genesTierMap.get( geneId ) != null ) // strip null values
                .map( geneId -> geneInfoRepository.findByGeneId( geneId ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toMap( g -> g, g -> genesTierMap.get( g.getGeneId() ) ) );
    }

    @Override
    public Map<GeneInfo, PrivacyLevelType> deserializeGenesPrivacyLevels( Map<Integer, PrivacyLevelType> genesPrivacyLevelMap ) {
        return genesPrivacyLevelMap.keySet().stream()
                .filter( geneId -> genesPrivacyLevelMap.get( geneId ) != null ) // strip null values
                .map( geneId -> geneInfoRepository.findByGeneId( geneId ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toMap( g -> g, g -> genesPrivacyLevelMap.get( g.getGeneId() ) ) );
    }

    @Override
    public void updateGenes() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();
        log.info( "Updating genes..." );
        for ( Taxon taxon : taxonService.findByActiveTrue() ) {
            try {
                Set<GeneInfo> data;
                if ( cacheSettings.isLoadFromDisk() ) {
                    Resource resource = cacheSettings.getGeneFilesLocation().createRelative( FilenameUtils.getName( taxon.getGeneUrl().getPath() ) );
                    log.info( MessageFormat.format( "Updating genes for {0} from {1}.", taxon, resource ) );
                    data = geneInfoParser.parse( taxon, new GZIPInputStream( resource.getInputStream() ) );
                } else {
                    log.info( MessageFormat.format( "Loading genes for {0} from {1}.",
                            taxon, taxon.getGeneUrl() ) );
                    data = geneInfoParser.parse( taxon, taxon.getGeneUrl() );
                }
                log.info( MessageFormat.format( "Done parsing genes for {0}.", taxon ) );
                geneInfoRepository.save( data );
                log.info( MessageFormat.format( "Done updating genes for {0}.", taxon ) );
            } catch ( ParseException | IOException e ) {
                log.error( MessageFormat.format( "Issue loading genes for {0}.", taxon ), e );
            }
        }
        log.info( MessageFormat.format( "Finished updating {0} genes.", geneInfoRepository.count() ) );
    }

    @Override
    public void updateGeneOrthologs() {
        log.info( MessageFormat.format( "Updating gene orthologs from {0}...", applicationSettings.getCache().getOrthologFile() ) );

        Set<Integer> supportedTaxons = taxonService.loadAll()
                .stream()
                .map( taxon -> taxon.getId() )
                .collect( Collectors.toSet() );

        List<GeneOrthologsParser.Record> records;
        try {
            records = geneOrthologsParser.parse( applicationSettings.getCache().getOrthologFile().getInputStream() );
        } catch ( IOException e ) {
            log.error( e );
            return;
        }

        for ( GeneOrthologsParser.Record record : records ) {
            // skip non-ortholog relationships
            if ( !record.getRelationship().equals( "Ortholog" ) )
                continue;

            // skip unsupported taxons
            if ( !supportedTaxons.contains( record.getTaxonId() ) || !supportedTaxons.contains( record.getOrthologTaxonId() ) )
                continue;

            GeneInfo gene = geneInfoRepository.findByGeneId( record.getGeneId() );
            GeneInfo ortholog = geneInfoRepository.findByGeneId( record.getOrthologId() );

            // skip genes or orthologs not stored in the database
            if ( gene == null || ortholog == null ) {
                log.info( MessageFormat.format( "Cannot add ortholog relationship between {0} and {1} since either or both gene are missing from the database.",
                        record.getGeneId(), record.getOrthologId() ) );
                continue;
            }

            gene.getOrthologs().add( ortholog );
            geneInfoRepository.save( gene );
        }
        log.info( "Done updating gene orthologs." );
    }

    private <T> boolean addAll( Collection<SearchResult<T>> container, Collection<T> newValues, GeneMatchType match, int maxSize ) {

        for ( T newValue : newValues ) {
            if ( maxSize == -1 || container.size() < maxSize ) {
                container.add( new SearchResult<>( match, newValue ) );
            } else {
                return true;
            }
        }

        return false;
    }
}
