package ubc.pavlab.rdp.repositories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.DatabaseMigrationConfig;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@Import(DatabaseMigrationConfig.class)
public class TaxonRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaxonRepository taxonRepository;

    @Test
    public void findByActiveTrue_thenReturnOnlyActive() {
        Taxon taxon1 = createTaxon( 1 );
        taxon1.setActive( false );
        Taxon taxon2 = createTaxon( 2 );
        taxon2.setActive( true );
        Taxon taxon3 = createTaxon( 3 );
        taxon3.setActive( true );

        entityManager.persist( taxon1 );
        entityManager.persist( taxon2 );
        entityManager.persist( taxon3 );

        entityManager.flush();

        Collection<Taxon> taxons = taxonRepository.findByActiveTrueOrderByOrdering();

        assertThat( taxons ).containsExactly( taxon2, taxon3 );

    }

}
