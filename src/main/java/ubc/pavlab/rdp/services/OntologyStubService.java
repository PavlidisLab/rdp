package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;

import java.util.*;

@Service
@CommonsLog
public class OntologyStubService {

    private final OntologyRepository ontologyRepository;

    @Autowired
    public OntologyStubService( OntologyRepository ontologyRepository ) {
        this.ontologyRepository = ontologyRepository;
    }

    /**
     * Create stubs for testing purposes.
     * <p>
     * Fake data for now, we have not fully implemented the model/repository
     *
     * @return a list of created stubs
     */
    @Transactional
    public void createStubs() {
        if ( ontologyRepository.existsByName( "research-models" ) ) {
            log.info( "Ontology stubs already created, skipping..." );
            return;
        }

        int termId = 0;

        Ontology researchModelOntology = Ontology.builder( "research-models" )
                .active( true )
                .ordering( 2 )
                .build();

        SortedSet<OntologyTermInfo> researchModelTerms = new TreeSet<>( OntologyTermInfo.getComparator() );
        researchModelTerms.add( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId )
                .ordering( 1 )
                .name( "patient-centric" )
                .isGroup( true )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId )
                        .active( true )
                        .ordering( 1 )
                        .name( "pdx-mouse" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 2 ).name( "pdx-fish" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 3 ).name( "pdx-other" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 4 ).name( "primary-organoid" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 5 ).name( "primary-co-culture" ).build() )
                .build() );

        researchModelTerms.add( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId )
                .ordering( 2 )
                .name( "disease-centric" )
                .isGroup( true )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 1 ).name( "transgenic-mouse" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 2 ).name( "transgenic-fish" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 3 ).name( "transgenic-other" ).build() )
                .build() );

        researchModelTerms.add( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId )
                .ordering( 3 )
                .name( "gene-centric" )
                .isGroup( true )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 1 ).name( "mouse" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 2 ).name( "rat" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 3 ).name( "frog" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 4 ).name( "zebrafish" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 5 ).name( "fly" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 6 ).name( "worm" ).build() )
                .subTerm( OntologyTermInfo.builder( researchModelOntology, "RMODEL:" + ++termId ).active( true ).ordering( 7 ).name( "yeast" ).build() )
                .build() );

        for ( OntologyTermInfo ontologyTermInfo : new HashSet<>( researchModelTerms ) ) {
            researchModelTerms.addAll( ontologyTermInfo.getSubTerms() );
        }

        researchModelOntology.getTerms().addAll( researchModelTerms );

        researchModelOntology = ontologyRepository.saveAndFlush( researchModelOntology );

        termId = 0;
        Ontology researchTechnologyOntology = Ontology.builder( "research-technology" )
                .active( true )
                .ordering( 3 )
                .build();
        SortedSet<OntologyTermInfo> researchTechnologyTerms = new TreeSet<>( OntologyTermInfo.getComparator() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 1 ).name( "high-throughput" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 2 ).name( "cell-therapy" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 3 ).name( "proteomics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 4 ).name( "genomics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 5 ).name( "genome-editing" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 6 ).name( "metabolomics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 7 ).name( "epigenetics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder( researchTechnologyOntology, "RTECH:" + ++termId ).active( true ).ordering( 8 ).name( "bioinformatics" ).build() );
        researchTechnologyOntology.getTerms().addAll( researchTechnologyTerms );

        researchTechnologyOntology = ontologyRepository.saveAndFlush( researchTechnologyOntology );

        termId = 0;
        Ontology tumorTissueOntology = Ontology.builder( "tumor-tissues" )
                .active( true )
                .ordering( 1 )
                .build();

        SortedSet<OntologyTermInfo> tumorTissueTerms = new TreeSet<>( OntologyTermInfo.getComparator() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 1 ).name( "brain" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 2 ).name( "leukemia-lymphoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( false ).ordering( 3 ).name( "neuroblastoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 4 ).name( "retinoblastoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 5 ).name( "sarcoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 6 ).name( "wilms" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 7 ).name( "rhabdho-myosarcoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( true ).ordering( 8 ).name( "hepatoblastoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( false ).ordering( 9 ).name( "germ-cell" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder( tumorTissueOntology, "TISSUE:" + ++termId ).hasIcon( false ).ordering( 10 ).name( "unspecified" ).active( true ).build() );
        tumorTissueOntology.getTerms().addAll( tumorTissueTerms );

        tumorTissueOntology = ontologyRepository.saveAndFlush( tumorTissueOntology );
    }

    public List<Ontology> findAllOntologyStubs() {
        return ontologyRepository.findAllByNameIn( Arrays.asList( "research-models", "research-technology", "tumor-tissues" ) );
    }
}
