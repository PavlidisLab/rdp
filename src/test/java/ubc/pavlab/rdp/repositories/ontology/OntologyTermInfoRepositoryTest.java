package ubc.pavlab.rdp.repositories.ontology;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@DataJpaTest
public class OntologyTermInfoRepositoryTest {

    @Autowired
    private OntologyTermInfoRepository ontologyTermInfoRepository;

    @Test
    public void findAllByActiveTrueAndOntologyActiveTrueAndObsoleteFalseAndSynonymsContainingAnyOfIgnoreCase() {
    }
}