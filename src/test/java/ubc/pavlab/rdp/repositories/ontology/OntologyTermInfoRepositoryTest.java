package ubc.pavlab.rdp.repositories.ontology;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class OntologyTermInfoRepositoryTest {

    @Autowired
    private OntologyRepository ontologyRepository;

    @Autowired
    private OntologyTermInfoRepository ontologyTermInfoRepository;

    @Test
    public void test() {
        Ontology ont = Ontology.builder( "mondo" ).build();
        OntologyTermInfo root1 = OntologyTermInfo.builder( ont, "TERM:000001" )
                .subTerm( OntologyTermInfo.builder( ont, "TERM:000002" ).build() )
                .subTerm( OntologyTermInfo.builder( ont, "TERM:000003" )
                        .subTerm( OntologyTermInfo.builder( ont, "TERM:000004" ).active( true ).build() )
                        .active( true )
                        .build() )
                .build();
        OntologyTermInfo root2 = OntologyTermInfo.builder( ont, "TERM:000005" ).active( true ).build();
        ont.getTerms().add( root1 );
        ont.getTerms().add( root2 );
        ontologyRepository.save( ont );
        assertThat( ontologyTermInfoRepository.findAll() ).hasSize( 5 );
        assertThat( ontologyTermInfoRepository.findAllByOntologyAndActiveAndSuperTermsEmpty( ont ) )
                .extracting( "termId" )
                .containsExactlyInAnyOrder( "TERM:000003", "TERM:000005" );
    }

}