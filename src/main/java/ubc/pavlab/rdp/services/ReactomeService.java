package ubc.pavlab.rdp.services;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.CollectionUtils;
import ubc.pavlab.rdp.util.ProgressCallback;
import ubc.pavlab.rdp.util.ProgressUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
@Service
@CommonsLog
public class ReactomeService {

    private final OntologyService ontologyService;

    private final ApplicationSettings applicationSettings;

    private final RestTemplate restTemplate;

    @Autowired
    public ReactomeService( OntologyService ontologyService, ApplicationSettings applicationSettings, @Qualifier("reactomeRestTemplate") RestTemplate restTemplate ) {
        this.ontologyService = ontologyService;
        this.applicationSettings = applicationSettings;
        this.restTemplate = restTemplate;
    }

    /**
     * @return
     */
    @Nullable
    public Ontology findPathwaysOntology() {
        return ontologyService.findByName( applicationSettings.getOntology().getReactomePathwaysOntologyName() );
    }

    /**
     * Import Reactome Human Pathways into the persistent storage.
     *
     * @return the Reactome ontology that was imported into the persistent storage
     * @throws ReactomeException if any operation with the data files fail, in which case the whole transaction will be
     *                           rolled back.
     * @see ApplicationSettings.OntologySettings#getReactomePathwaysOntologyName()
     */
    @Transactional(rollbackFor = ReactomeException.class)
    public Ontology importPathwaysOntology() throws ReactomeException {
        Ontology ontology = ontologyService.findByName( applicationSettings.getOntology().getReactomePathwaysOntologyName() );
        if ( ontology != null ) {
            throw new IllegalArgumentException( "The Reactome pathways ontology has already been imported." );
        } else {
            return ontologyService.createNoAuth( importOrUpdatePathwaysOntology() );
        }
    }

    /**
     * Update Reactome pathways ontology if already setup in the persistent storage.
     *
     * @return the Reactome pathway ontology if it exists, otherwise null
     * @throws ReactomeException if any operation with the data files fail, in which case the whole transaction will be
     *                           rolled back.
     */
    @Nullable
    @Transactional(rollbackFor = ReactomeException.class)
    public Ontology updatePathwaysOntology() throws ReactomeException {
        if ( ontologyService.existsByName( applicationSettings.getOntology().getReactomePathwaysOntologyName() ) ) {
            log.info( "Updating Reactome Pathways..." );
            Ontology ontology = ontologyService.updateNoAuth( importOrUpdatePathwaysOntology() );
            log.info( "Propagating subtree activation..." );
            int numActivated = ontologyService.propagateSubtreeActivation( ontology );
            if ( numActivated > 0 ) {
                log.info( String.format( "%d terms got activated in %s.", numActivated, ontology ) );
            } else {
                log.info( "No subtree needed activation propagation." );
            }
            return ontology;
        } else {
            return null;
        }
    }

    private Ontology importOrUpdatePathwaysOntology() throws ReactomeException {
        String ontologyName = applicationSettings.getOntology().getReactomePathwaysOntologyName();
        Ontology ontology = ontologyService.findByName( ontologyName );
        if ( ontology == null ) {
            ontology = Ontology.builder( ontologyName ).build();
        }

        Map<String, List<String>> pathwayIdToExternalIds = new HashMap<>();
        try ( CSVParser parser = CSVFormat.TDF.withCommentMarker( '#' ).withFirstRecordAsHeader().parse( new InputStreamReader( applicationSettings.getOntology().getReactomeStableIdentifiersFile().getInputStream() ) ) ) {
            for ( CSVRecord record : parser ) {
                String pathwayId = record.get( 0 );
                List<String> externalIds = Arrays.asList( record.get( 1 ).split( "," ) );
                pathwayIdToExternalIds.put( pathwayId, externalIds );
            }
        } catch ( IOException e ) {
            throw new ReactomeException( String.format( "Failed to open the Reactome stable identifiers file %s.", applicationSettings.getOntology().getReactomeStableIdentifiersFile() ),
                    e );
        }

        Map<String, OntologyTermInfo> existingTermByTermId = ontology.getTerms().stream()
                .collect( Collectors.toMap( OntologyTermInfo::getTermId, Function.identity() ) );

        Map<String, OntologyTermInfo> pathwayById = new HashMap<>();
        try ( CSVParser parser = CSVFormat.TDF.parse( new InputStreamReader( applicationSettings.getOntology().getReactomePathwaysFile().getInputStream() ) ) ) {
            for ( CSVRecord record : parser ) {
                String pathwayId = record.get( 0 );
                String pathwayName = record.get( 1 );
                String pathwayTaxonScientificName = record.get( 2 );
                if ( pathwayTaxonScientificName.equals( "Homo sapiens" ) ) {
                    OntologyTermInfo oti = existingTermByTermId.get( pathwayId );
                    if ( oti == null ) {
                        oti = OntologyTermInfo.builder( ontology, pathwayId ).build();
                    }
                    if ( pathwayName.length() > OntologyTermInfo.MAX_NAME_LENGTH ) {
                        log.warn( String.format( "Name %s for Reactome Pathway %s is too long (%d characters), it will be truncated to %d characters.",
                                pathwayName, pathwayId, pathwayName.length(), OntologyTermInfo.MAX_NAME_LENGTH ) );
                        oti.setName( pathwayName.substring( 0, OntologyTermInfo.MAX_NAME_LENGTH ) );
                    } else {
                        oti.setName( pathwayName );
                    }
                    oti.getAltTermIds().clear();
                    oti.getAltTermIds().addAll( pathwayIdToExternalIds.getOrDefault( pathwayId, Collections.emptyList() ) );
                    oti.setSubTerms( new TreeSet<>() );
                    pathwayById.put( pathwayId, oti );
                }
            }
        } catch ( IOException e ) {
            throw new ReactomeException( String.format( "Failed to open the Reactome pathways file %s.", applicationSettings.getOntology().getReactomePathwaysFile() ), e );
        }

        try ( CSVParser parser = CSVFormat.TDF.parse( new InputStreamReader( applicationSettings.getOntology().getReactomePathwaysHierarchyFile().getInputStream() ) ) ) {
            for ( CSVRecord record : parser ) {
                OntologyTermInfo parentPathway = pathwayById.get( record.get( 0 ) );
                OntologyTermInfo pathway = pathwayById.get( record.get( 1 ) );
                if ( parentPathway != null && pathway != null ) {
                    parentPathway.getSubTerms().add( pathway );
                }
            }
        } catch ( IOException e ) {
            throw new ReactomeException( String.format( "Failed to open the Reactome pathways hierarchy file %s.", applicationSettings.getOntology().getReactomePathwaysHierarchyFile() ), e );
        }

        CollectionUtils.updateWithMap( ontology.getTerms(), OntologyTermInfo::getTermId, pathwayById );

        return ontology;
    }

    /**
     * Update term definitions of all the terms defined in the Reactome pathway ontology.
     *
     * @param progressCallback emits processed summations, or null to ignore
     * @throws ReactomeException if any operation with the ReactomeContent Service fails, in which case the whole
     *                           transaction will be rolled back.
     */
    @Transactional(rollbackFor = ReactomeException.class)
    public void updatePathwaySummations( @Nullable ProgressCallback progressCallback ) throws ReactomeException {
        String ontologyName = applicationSettings.getOntology().getReactomePathwaysOntologyName();
        Ontology ontology = ontologyService.findByName( ontologyName );
        if ( ontology == null ) {
            return;
        }

        StopWatch timer = StopWatch.createStarted();

        int maxPage = (int) Math.ceil( ontology.getTerms().size() / 20.0 );
        log.info( String.format( "Updating Reactome Pathways Summations, %d pages or 20 terms will be processed...", maxPage ) );

        for ( int i = 0; i < maxPage; i++ ) {
            Page<OntologyTermInfo> page = ontologyService.findAllTermsByOntologyIncludingInactive( ontology, PageRequest.of( i, 20 ) );
            URI queryIdsUri = UriComponentsBuilder.fromUri( applicationSettings.getOntology().getReactomeContentServiceUrl() )
                    .path( "/data/query/ids" ).build().toUri();
            ResponseEntity<ReactomeEntity[]> entity = restTemplate.postForEntity( queryIdsUri, new HttpEntity<>( String.join( ",", page.map( OntologyTermInfo::getTermId ).getContent() ) ), ReactomeEntity[].class );
            ProgressUtils.emitProgress( progressCallback, ( i * 20L ) + page.getNumberOfElements(), page.getTotalElements(), timer.getTime( TimeUnit.MILLISECONDS ) );
            if ( entity.getStatusCode().is2xxSuccessful() && entity.getBody() != null ) {
                Map<String, String> results = Arrays.stream( entity.getBody() )
                        .filter( e -> e.getSummation() != null && e.getSummation().size() > 0 )
                        .collect( Collectors.toMap( ReactomeEntity::getStId, e -> e.getSummation().get( 0 ).getText() ) );
                for ( OntologyTermInfo term : page ) {
                    if ( results.containsKey( term.getTermId() ) ) {
                        term.setDefinition( results.get( term.getTermId() ) );
                    }
                }
                ontologyService.saveTermsNoAuth( page );
            } else {
                log.error( String.format( "Unexpected response from Reactome content service: %s", entity ) );
            }
        }

        timer.stop();

        log.info( String.format( "Done updating Reactome Pathway Summations in %d ms.", timer.getTime( TimeUnit.MILLISECONDS ) ) );
    }

    @Data
    public static class ReactomeEntity {
        private String stId;
        private List<ReactomeSummation> summation;
    }

    @Data
    public static class ReactomeSummation {
        private String text;
    }
}
