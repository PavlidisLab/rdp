package ubc.pavlab.rdp.services;

import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author poirgui
 */
@Service
public class OntologyService {

    private static final List<Ontology> ontologies = new ArrayList<>();

    private static final List<OntologyTermInfo> terms = new ArrayList<>();

    static {
        int ontologyId = 0;
        int termId = 0;

        Ontology researchModelOntology = Ontology.builder()
                .id( ++ontologyId )
                .name( "research-models" )
                .active( true )
                .order( 2 )
                .build();
        SortedSet<OntologyTermInfo> researchModelTerms = new TreeSet<>( OntologyTermInfo.getComparator() );
        researchModelTerms.add( OntologyTermInfo.builder()
                .id( "RMODEL:" + ++termId )
                .order( 1 )
                .name( "patient-centric" )
                .isGroup( true )
                .ontology( researchModelOntology )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId )
                        .ontology( researchModelOntology )
                        .active( true )
                        .order( 1 )
                        .name( "pdx-mouse" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 2 ).name( "pdx-fish" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 3 ).name( "pdx-other" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 4 ).name( "primary-organoid" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 5 ).name( "primary-co-culture" ).build() )
                .build() );

        researchModelTerms.add( OntologyTermInfo.builder()
                .id( "RMODEL:" + ++termId )
                .order( 2 )
                .name( "disease-centric" )
                .isGroup( true )
                .ontology( researchModelOntology )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 1 ).name( "transgenic-mouse" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 2 ).name( "transgenic-fish" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 3 ).name( "transgenic-other" ).build() )
                .build() );

        researchModelTerms.add( OntologyTermInfo.builder()
                .id( "RMODEL:" + ++termId )
                .order( 3 )
                .name( "gene-centric" )
                .isGroup( true )
                .ontology( researchModelOntology )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 1 ).name( "mouse" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 2 ).name( "rat" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 3 ).name( "frog" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 4 ).name( "zebrafish" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 5 ).name( "fly" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 6 ).name( "worm" ).build() )
                .subTerm( OntologyTermInfo.builder().id( "RMODEL:" + ++termId ).ontology( researchModelOntology ).active( true ).order( 7 ).name( "yeast" ).build() )
                .build() );

        researchModelOntology.setTerms( researchModelTerms );

        ontologies.add( researchModelOntology );
        terms.addAll( researchModelTerms );

        Ontology researchTechnologyOntology = Ontology.builder()
                .name( "research-technology" )
                .active( true )
                .order( 3 )
                .build();
        SortedSet<OntologyTermInfo> researchTechnologyTerms = new TreeSet<>( OntologyTermInfo.getComparator() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 1 ).name( "high-throughput" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 2 ).name( "cell-therapy" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 3 ).name( "proteomics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 4 ).name( "genomics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 5 ).name( "genome-editing" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 6 ).name( "metabolomics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 7 ).name( "epigenetics" ).build() );
        researchTechnologyTerms.add( OntologyTermInfo.builder().id( "RTECH:" + ++termId ).ontology( researchTechnologyOntology ).order( 8 ).name( "bioinformatics" ).build() );
        researchTechnologyOntology.setTerms( researchTechnologyTerms );

        ontologies.add( researchTechnologyOntology );
        terms.addAll( researchTechnologyTerms );

        // fake data for now, we have not fully implemented the model/repository

        Ontology tumorTissueOntology = Ontology.builder()
                .id( ++ontologyId )
                .name( "tumor-tissues" )
                .active( true )
                .order( 1 )
                .build();

        SortedSet<OntologyTermInfo> tumorTissueTerms = new TreeSet<>( OntologyTermInfo.getComparator() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 1 ).name( "brain" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 2 ).name( "leukemia-lymphoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( false ).ontology( tumorTissueOntology ).order( 3 ).name( "neuroblastoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 4 ).name( "retinoblastoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 5 ).name( "sarcoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 6 ).name( "wilms" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 7 ).name( "rhabdho-myosarcoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( true ).ontology( tumorTissueOntology ).order( 8 ).name( "hepatoblastoma" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( false ).ontology( tumorTissueOntology ).order( 9 ).name( "germ-cell" ).active( true ).build() );
        tumorTissueTerms.add( OntologyTermInfo.builder().id( "TISSUE:" + ++termId ).hasIcon( false ).ontology( tumorTissueOntology ).order( 10 ).name( "unspecified" ).active( true ).build() );
        tumorTissueOntology.setTerms( tumorTissueTerms );

        ontologies.add( tumorTissueOntology );
        terms.addAll( tumorTissueTerms );
    }

    public Ontology findOntologyById( String id ) {
        return ontologies.stream()
                .filter( c -> c.getId().equals( id ) )
                .findFirst().orElse( null );
    }

    public List<Ontology> findAllOntologies() {
        return ontologies.stream()
                .sorted( Ontology.getComparator() )
                .collect( Collectors.toList() );
    }

    public OntologyTermInfo findTermInfoById( String id ) {
        return terms.stream()
                .filter( c -> c.getId().equals( id ) )
                .findFirst().orElse( null );
    }

    public List<OntologyTermInfo> findAllTerms() {
        return terms.stream()
                .sorted( OntologyTermInfo.getComparator() )
                .collect( Collectors.toList() );
    }

    public List<OntologyTermInfo> findAllTermsByTaxon( Taxon taxon ) {
        return findAllTerms().stream()
                .filter( term -> Objects.equals( term.getTaxon(), taxon ) )
                .collect( Collectors.toList() );
    }
}
