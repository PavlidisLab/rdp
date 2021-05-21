package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GeneInfoParser;
import ubc.pavlab.rdp.util.GeneOrthologsParser;
import ubc.pavlab.rdp.util.SearchResult;

import java.io.IOException;
import java.text.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;

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
    public void updateGenes() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();
        log.info( "Updating genes..." );
        for ( Taxon taxon : taxonService.findByActiveTrue() ) {
            try {
                List<GeneInfoParser.Record> data;
                if ( cacheSettings.isLoadFromDisk() ) {
                    Resource resource = cacheSettings.getGeneFilesLocation().createRelative( FilenameUtils.getName( taxon.getGeneUrl().getPath() ) );
                    log.info( MessageFormat.format( "Loading genes for {0} from {1}.", taxon, resource ) );
                    data = geneInfoParser.parse( new GZIPInputStream( resource.getInputStream() ) );
                } else {
                    log.info( MessageFormat.format( "Loading genes for {0} from {1}.",
                            taxon, taxon.getGeneUrl() ) );
                    data = geneInfoParser.parse( taxon.getGeneUrl() );
                }
                log.info( MessageFormat.format( "Done parsing genes for {0}.", taxon ) );
                // retrieve all relevant genes in a single database query
                Map<Integer, GeneInfo> foundGenesByGeneId = geneInfoRepository
                        .findAllByGeneIdIn( data.stream().map( GeneInfoParser.Record::getGeneId ).collect( Collectors.toList() ) )
                        .stream()
                        .collect( Collectors.toMap( GeneInfo::getGeneId, identity() ) );
                log.info( MessageFormat.format( "Done retrieving existing genes for {0}, will now proceed to update.", taxon ) );
                long numberOfGenesInTaxon = geneInfoRepository.countByTaxon( taxon );
                if ( foundGenesByGeneId.size() < numberOfGenesInTaxon ) {
                    log.warn( MessageFormat.format( "No information were found for {0} genes in {1}.", numberOfGenesInTaxon - foundGenesByGeneId.size(), taxon ) );
                }
                Set<GeneInfo> geneData = data.stream()
                        .map( record -> {
                            GeneInfo gene = foundGenesByGeneId.getOrDefault( record.getGeneId(), new GeneInfo() );
                            gene.setTaxon( taxon );
                            gene.setGeneId( record.getGeneId() );
                            gene.setSymbol( record.getSymbol() );
                            gene.setAliases( record.getSynonyms() );
                            gene.setName( record.getDescription() );
                            gene.setModificationDate( record.getModificationDate() );
                            return gene;
                        } ).collect( Collectors.toSet() );
                geneInfoRepository.save( geneData );
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

        DecimalFormat geneIdFormat = new DecimalFormat();
        geneIdFormat.setGroupingUsed( false );

        Set<Integer> supportedTaxons = taxonService.loadAll()
                .stream()
                .map( Taxon::getId )
                .collect( Collectors.toSet() );

        List<GeneOrthologsParser.Record> records;
        try {
            records = geneOrthologsParser.parse( applicationSettings.getCache().getOrthologFile().getInputStream() );
        } catch ( IOException e ) {
            log.error( e );
            return;
        }

        Map<Integer, List<GeneOrthologsParser.Record>> recordByGeneId = records.stream()
                .filter( record -> record.getRelationship().equals( "Ortholog" ) )
                .filter( record -> supportedTaxons.contains( record.getTaxonId() ) && supportedTaxons.contains( record.getOrthologTaxonId() ) )
                .collect( groupingBy( GeneOrthologsParser.Record::getGeneId ) );

        for ( Integer geneId : recordByGeneId.keySet() ) {
            GeneInfo gene = geneInfoRepository.findByGeneIdWithOrthologs( geneId );
            if ( gene == null ) {
                log.info( MessageFormat.format( "Ignoring orthologs for {0} since it's missing from the database.",
                        geneIdFormat.format( geneId ) ) );
                continue;
            }
            gene.getOrthologs().clear();
            for ( GeneOrthologsParser.Record record : recordByGeneId.get( geneId ) ) {
                GeneInfo ortholog = geneInfoRepository.findByGeneId( record.getOrthologId() );
                if ( ortholog == null ) {
                    log.info( MessageFormat.format( "Cannot add ortholog relationship between {0} and {1} since the latter is missing from the database.",
                            geneIdFormat.format( geneId ), geneIdFormat.format( record.getOrthologId() ) ) );
                    continue;
                }
                gene.getOrthologs().add( ortholog );
            }
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
