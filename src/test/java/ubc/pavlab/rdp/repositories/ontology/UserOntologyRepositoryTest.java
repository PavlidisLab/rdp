package ubc.pavlab.rdp.repositories.ontology;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.JpaAuditingConfig;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.model.ontology.UserOntologyTerm;
import ubc.pavlab.rdp.repositories.UserRepository;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(JpaAuditingConfig.class)
public class UserOntologyRepositoryTest {

    @Autowired
    private OntologyRepository ontologyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    public void delete_whenUserHasTerm_thenSetTermInfoToNull() {
        Ontology ontology = Ontology.builder( "ABC" ).build();
        OntologyTermInfo subTerm = OntologyTermInfo.builder( ontology, "ABC:124" )
                .name( "subterm of ABC:123" )
                .build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "ABC:123" )
                .name( "abc" )
                .subTerm( subTerm )
                .build();
        ontology.getTerms().add( term );
        ontology.getTerms().add( subTerm );
        ontologyRepository.saveAndFlush( ontology );

        assertThat( term.getId() ).isNotNull();
        entityManager.refresh( term );

        User user = createUnpersistedUser();
        UserOntologyTerm uo = UserOntologyTerm.fromOntologyTermInfo( user, term );
        user.getUserOntologyTerms().add( uo );
        user = userRepository.saveAndFlush( user );
        Instant a = Instant.now().minus( 500, ChronoUnit.MILLIS );
        Instant b = Instant.now();
        assertThat( user.getUserOntologyTerms() ).allSatisfy( userTerm -> {
            assertThat( userTerm.getCreatedAt() ).isBetween( a, b );
        } );

        ontology.getTerms().remove( term );
        ontologyRepository.saveAndFlush( ontology );
        entityManager.flush();
        entityManager.refresh( ontology );
        assertThat( ontology.getTerms() ).doesNotContain( term );

        entityManager.refresh( uo );
        assertThat( uo.getTermInfo() ).isNull();
    }
}
