package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;

import static ubc.pavlab.rdp.util.TestUtils.createGene;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

@RunWith(SpringRunner.class)
@DataJpaTest
public class GeneInfoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GeneInfoRepository geneInfoRepository;

    @Before
    public void setUp() {
        Taxon taxon = createTaxon( 1 );
        for (int i = 1; i <= 3; i++) {
            GeneInfo g = createGene(1, taxon);
            g.setSymbol( "Gene" + g.getGeneId() + "Symbol" );
            g.setName( "Gene" + g.getGeneId() + "Name" );
            g.setAliases( "Gene" + g.getGeneId() + "Alias" );
            entityManager.persist(g);
        }
    }

}
