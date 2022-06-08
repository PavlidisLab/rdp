package ubc.pavlab.rdp.services;

import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
@Service
public class ReactomeService {

    private final OntologyService ontologyService;

    private final ApplicationSettings applicationSettings;

    @Autowired
    public ReactomeService( OntologyService ontologyService, ApplicationSettings applicationSettings ) {
        this.ontologyService = ontologyService;
        this.applicationSettings = applicationSettings;
    }

    /**
     * Import Reactome Human Pathways into the "reactome" ontology.
     */
    @SneakyThrows(MalformedURLException.class)
    @Transactional
    public Ontology importReactomePathways() throws IOException {
        Ontology ontology = ontologyService.findByName( "reactome" );
        if ( ontology == null ) {
            ontology = Ontology.builder( "reactome" ).build();
        }

        Map<String, List<String>> pathwayIdToExternalIds = new HashMap<>();
        try ( CSVParser parser = CSVFormat.TDF.withCommentMarker( '#' ).withFirstRecordAsHeader().parse( new InputStreamReader( applicationSettings.getOntology().getReactomeStableIdentifiersFile().getInputStream() ) ) ) {
            for ( CSVRecord record : parser ) {
                String pathwayId = record.get( 0 );
                List<String> externalIds = Arrays.asList( record.get( 1 ).split( "," ) );
                pathwayIdToExternalIds.put( pathwayId, externalIds );
            }
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
                        oti = OntologyTermInfo.builder( ontology, pathwayId )
                                .name( pathwayName )
                                .build();
                    }
                    oti.setName( pathwayName );
                    oti.getAltTermIds().clear();
                    oti.getAltTermIds().addAll( pathwayIdToExternalIds.getOrDefault( pathwayId, Collections.emptyList() ) );
                    oti.setSubTerms( new TreeSet<>() );
                    pathwayById.put( pathwayId, oti );
                }
            }
        }

        try ( CSVParser parser = CSVFormat.TDF.parse( new InputStreamReader( applicationSettings.getOntology().getReactomePathwaysHierarchyFile().getInputStream() ) ) ) {
            for ( CSVRecord record : parser ) {
                OntologyTermInfo parentPathway = pathwayById.get( record.get( 0 ) );
                OntologyTermInfo pathway = pathwayById.get( record.get( 1 ) );
                if ( parentPathway != null && pathway != null ) {
                    parentPathway.getSubTerms().add( pathway );
                }
            }
        }

        ontology.getTerms().removeIf( p -> !pathwayById.containsKey( p.getTermId() ) );
        ontology.getTerms().addAll( pathwayById.values() );

        ontology = ontologyService.save( ontology );

        ontologyService.activate( ontology );

        return ontology;
    }
}
