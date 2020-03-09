package ubc.pavlab.rdp.services;

import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GeneInfoParser;
import ubc.pavlab.rdp.util.SearchResult;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("geneService")
@CommonsLog
public class GeneInfoServiceImpl implements GeneInfoService {

    public static final String GENE_ORTHOLOG_URL = "https://ftp.ncbi.nlm.nih.gov/gene/DATA/gene_orthologs.gz";

    @Autowired
    GeneInfoRepository geneInfoRepository;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Override
    public GeneInfo load( Integer id ) {
        return geneInfoRepository.findOne(id);
    }

    @Override
    public Collection<GeneInfo> load( Collection<Integer> ids ) {
        return geneInfoRepository.findAllByIdIn (ids);
    }

    @Override
    public GeneInfo findBySymbolAndTaxon( String symbol, Taxon taxon ) {
        return geneInfoRepository.findBySymbolAndTaxon (symbol, taxon);
    }

    @Override
    public Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon ) {
        return geneInfoRepository.findBySymbolInAndTaxon( symbols ,taxon );
    }

    @Override
    public Collection<SearchResult<GeneInfo>> autocomplete( String query, Taxon taxon, int maxResults ) {
        Collection<SearchResult<GeneInfo>> results = new LinkedHashSet<>();

        if ( addAll( results, geneInfoRepository.findAllBySymbol (query), GeneMatchType.EXACT_SYMBOL, maxResults ) ) {
            return results;
        }

        if ( addAll( results, geneInfoRepository.findAllBySymbolStartingWithIgnoreCase(query),
                GeneMatchType.SIMILAR_SYMBOL, maxResults ) ) {
            return results;
        }

        if ( addAll( results, geneInfoRepository.findAllByNameStartingWithIgnoreCase(query),
                GeneMatchType.SIMILAR_NAME, maxResults ) ) {
            return results;
        }

        addAll( results, geneInfoRepository.findAllByAliasesContainingIgnoreCase(query),
                GeneMatchType.SIMILAR_ALIAS, maxResults );

        return results;
    }

    @Override
    public Map<GeneInfo, TierType> deserializeGenesTiers( Map<Integer, TierType> genesTierMap ) {
        return load( genesTierMap.keySet() ).stream().filter( Objects::nonNull )
                .collect( Collectors.toMap( g -> g, g -> genesTierMap.get( g.getGeneId() ) ) );
    }

    @Override
    public Map<GeneInfo, Optional<PrivacyLevelType>> deserializeGenesPrivacyLevels( Map<Integer, PrivacyLevelType> genesPrivacyLevelMap ) {
        return load( genesPrivacyLevelMap.keySet() ).stream().filter( Objects::nonNull )
                .collect( Collectors.toMap( g -> g, g -> Optional.ofNullable( genesPrivacyLevelMap.get( g.getGeneId() ) ) ) );
    }

    @Autowired
    GeneInfoParser geneInfoParser;

    @Autowired
    UserService userService;

    /**
     * Genes are updated every month or so.
     */
    @Scheduled(fixedRate = 2592000000L)
    @Transactional
    void updateGenes() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();

        if ( cacheSettings.isEnabled() ) {
            log.info( "Updating genes..." );
            for ( Taxon taxon : taxonService.findByActiveTrue() ) {

                try {
                    Set<GeneInfo> data;
                    if ( cacheSettings.isLoadFromDisk() ) {
                        Path path = Paths.get( cacheSettings.getGeneFilesLocation(), FilenameUtils.getName( taxon.getGeneUrl().getPath() ) );
                        log.info( "Loading genes for " + taxon.toString() + " from disk: " + path.toAbsolutePath() );
                        data = geneInfoParser.parse( taxon, path.toFile() );
                    } else {
                        log.info( MessageFormat.format("Loading genes for {0} from URL {1}.",
                                taxon, taxon.getGeneUrl().getPath() ));
                        data = geneInfoParser.parse( taxon, taxon.getGeneUrl() );
                    }
                    log.info( MessageFormat.format("Done parsing genes for {0}.", taxon ));
                    geneInfoRepository.save( data );
                    log.info (MessageFormat.format("Done updating genes for {0}.", taxon ));
                } catch ( ParseException e ) {
                    log.error( MessageFormat.format( "Issue loading genes for {0}.", taxon ), e );
                }
            }

            log.info( "Now updating user genes..." );
            for ( User user : userService.findAll()) {
                for ( UserGene userGene : user.getUserGenes().values() ) {
                    Gene cachedGene = geneInfoRepository.findByGeneIdAndTaxon( userGene.getGeneId(), userGene.getTaxon() );
                    if ( cachedGene != null ) {
                        userGene.updateGene( cachedGene );
                    }
                }
            }

            log.info( MessageFormat.format( "Finished updating {0} genes. Next update is scheduled in 30 days from now.",
                    geneInfoRepository.count() ) );
        }
    }

    /**
     * Update gene orthologs.
     */
    @Transactional
    @Scheduled(fixedRate = 2592000000L)
    void updateGeneOrthologs() {
        if (applicationSettings.getCache().isEnabled()) {
            InputStream is;
            if (applicationSettings.getCache().isLoadFromDisk()) {
                try {
                    is = new FileInputStream (applicationSettings.getCache().getOrthologFile());
                } catch ( FileNotFoundException e ) {
                    log.error(e);
                    return;
                }
                log.info(MessageFormat.format("Updating gene orthologs from {0}...",
                        applicationSettings.getCache().getOrthologFile()));
            } else {
                try {
                    is = new URL(GENE_ORTHOLOG_URL).openStream();
                } catch ( IOException e ) {
                    log.error(e);
                    return;
                }
                log.info(MessageFormat.format("Updating gene orthologs from {0}...",
                        GENE_ORTHOLOG_URL));
            }

            Set<Integer> supportedTaxons = taxonService.loadAll()
                    .stream()
                    .map(taxon -> taxon.getId())
                    .collect(Collectors.toSet());

            new BufferedReader (new InputStreamReader(new GZIPInputStream(is)))
                    .lines()
                    .skip(1) // skip the TSV header
                    .map(line -> line.split ("\t"))
                    .forEach(line -> {
                        Integer taxonId = Integer.parseInt(line[0]);
                        Integer geneId = Integer.parseInt(line[1]);
                        String relationship = line[2];
                        Integer orthologTaxonId = Integer.parseInt(line[3]);
                        Integer orthologId = Integer.parseInt(line[4]);

                        // skip non-ortholog relationships
                        if (!relationship.equals ("Ortholog"))
                            return;

                        // skip unsupported taxons
                        if (!supportedTaxons.contains (taxonId) || !supportedTaxons.contains (orthologTaxonId))
                            return;

                        GeneInfo gene = geneInfoRepository.findByGeneId( geneId );
                        GeneInfo ortholog = geneInfoRepository.findByGeneId( orthologId );

                        // skip genes or orthologs not stored in the database
                        if (gene == null || ortholog == null)
                            return;

                        gene.getOrthologs().add(ortholog);
                        geneInfoRepository.save(gene);
                    });
            log.info("Done updating gene orthologs.");
        }
    }

    private <T> boolean addAll( Collection<SearchResult<T>> container, Collection<T> newValues, GeneMatchType match,
            int maxSize ) {

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
