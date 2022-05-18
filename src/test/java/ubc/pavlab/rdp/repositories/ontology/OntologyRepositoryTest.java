package ubc.pavlab.rdp.repositories.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import javax.persistence.EntityManager;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test also include tests for {@link OntologyTermInfoRepository} since the two repositories are closely linked.
 *
 * @author poirigui
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@CommonsLog
public class OntologyRepositoryTest {

    @Autowired
    private OntologyRepository ontologyRepository;
    @Autowired
    private OntologyTermInfoRepository ontologyTermInfoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void findAllByActiveTrueAndOntologyActiveTrue() {
        ontologyTermInfoRepository.findAllByActiveTrueAndOntologyActiveTrue();
    }

    @Test
    public void saveTermInfo() {
        Ontology ontology = ontologyRepository.save( Ontology.builder().name( "ABC" ).build() );
        ontologyTermInfoRepository.saveAndFlush( OntologyTermInfo.builder( ontology, "ABC:123" )
                .name( "abc" )
                .build() );
    }

    @Test
    public void saveTermInfo_whenTermHasSubTerms_thenCascadeToSubTerms() {
        Ontology ontology = ontologyRepository.save( Ontology.builder().name( "ABC" ).build() );
        OntologyTermInfo subTerm = OntologyTermInfo.builder( ontology, "ABC:124" )
                .name( "subterm of ABC:123" )
                .build();
        OntologyTermInfo term = ontologyTermInfoRepository.saveAndFlush( OntologyTermInfo.builder( ontology, "ABC:123" )
                .name( "abc" )
                .subTerm( subTerm )
                .build() );
        entityManager.refresh( ontology );
        assertThat( ontology.getTerms() ).contains( term, subTerm );
        entityManager.refresh( subTerm );
        assertThat( subTerm.getSuperTerms() ).containsExactly( term );
        assertThat( term.getSubTerms() ).containsExactly( subTerm );
    }

    @Test
    public void save_whenTermHasSubTerms_thenCascadeToSubTerms() {
        Ontology ontology = Ontology.builder().name( "ABC" ).build();
        OntologyTermInfo subTerm = OntologyTermInfo.builder( ontology, "ABC:124" )
                .name( "subterm of ABC:123" )
                .build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "ABC:123" )
                .name( "abc" )
                .subTerm( subTerm )
                .build();

        ontology.setTerms( new TreeSet<>() );
        ontology.getTerms().add( term );
        ontology.getTerms().add( subTerm );

        ontology = ontologyRepository.saveAndFlush( ontology );
        assertThat( ontology.getTerms() ).contains( term, subTerm );
    }

    @Test
    public void save_whenTermIsOrphaned_thenSubTermsAreUpdatedAccordingly() {
        Ontology ontology = Ontology.builder().name( "ABC" ).build();
        OntologyTermInfo subTerm = OntologyTermInfo.builder( ontology, "ABC:124" )
                .name( "subterm of ABC:123" )
                .build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "ABC:123" )
                .name( "abc" )
                .subTerm( subTerm )
                .build();

        ontology.setTerms( new TreeSet<>() );
        ontology.getTerms().add( term );
        ontology.getTerms().add( subTerm );

        ontology = ontologyRepository.saveAndFlush( ontology );
        assertThat( ontology.getTerms() ).contains( term, subTerm );
        entityManager.refresh( subTerm );
        assertThat( subTerm.getSuperTerms() ).contains( term );

        ontology.getTerms().remove( term );
        ontologyRepository.saveAndFlush( ontology );
        entityManager.refresh( subTerm );
        assertThat( subTerm.getSuperTerms() ).isEmpty();
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void save_whenOntologyIsNull_thenFail() {
        ontologyTermInfoRepository.saveAndFlush( OntologyTermInfo.builder( null, "ABC:123" )
                .name( "abc" )
                .build() );
    }

    @Test
    public void delete_whenTermHasSubTerm_thenCascadeAccordingly() {
        Ontology ontology = Ontology.builder().name( "ABC" ).build();
        assertThat( ontology.getTerms() ).isNotNull();
        OntologyTermInfo subTerm = OntologyTermInfo.builder( ontology, "ABC:124" )
                .name( "subterm of ABC:123" )
                .build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "ABC:123" )
                .name( "abc" )
                .subTerm( subTerm )
                .build();

        ontology.setTerms( new TreeSet<>() );
        ontology.getTerms().add( term );
        ontology.getTerms().add( subTerm );

        ontology = ontologyRepository.saveAndFlush( ontology );
        assertThat( ontology.getTerms() ).contains( term, subTerm );

        ontologyRepository.delete( ontology );
        ontologyRepository.flush();

        assertThat( ontologyRepository.findOne( ontology.getId() ) ).isNull();
        assertThat( ontologyTermInfoRepository.findOne( term.getId() ) ).isNull();
        assertThat( ontologyTermInfoRepository.findOne( subTerm.getId() ) ).isNull();
    }
}