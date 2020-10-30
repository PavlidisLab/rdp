package ubc.pavlab.rdp.repositories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class TaxonRepositoryTest {

    @Autowired
    private TaxonRepository taxonRepository;

    @Test
    public void findByActiveTrue_thenReturnOnlyActive() {
        Taxon humanTaxon = createTaxon( 9606 );
        Collection<Taxon> taxon = taxonRepository.findByActiveTrueOrderByOrdering();
        assertThat( taxon ).containsExactly( humanTaxon );
    }

}
