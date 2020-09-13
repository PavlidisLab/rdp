package ubc.pavlab.rdp.services;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubc.pavlab.rdp.util.TestUtils.*;

@RunWith(SpringRunner.class)
public class UserGeneServiceImplTest {

    @TestConfiguration
    static class UserGeneServiceImplTestContextConfiguration {

        @Bean
        public UserGeneService userGeneService() {
            return new UserGeneServiceImpl();
        }

    }

    @Autowired
    private UserGeneService userGeneService;

    @MockBean
    private TaxonRepository taxonRepository;

    @MockBean
    private GeneInfoRepository geneInfoRepository;

    @MockBean
    private UserGeneRepository userGeneRepository;

    @MockBean
    private TierService tierService;

    @Test
    public void updateUserGenes_withExistingUser_thenUserGeneAreUpdated() {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo gene = createGene( 1, humanTaxon );
        UserGene userGene = createUserGene( 1, gene, user, TierType.TIER1 );
        userGene.setGeneInfo (gene);
        when( userGeneRepository.findAllWithGeneInfo() ).thenReturn( Lists.newArrayList( userGene ) );
        when( geneInfoRepository.findByGeneIdAndTaxon( gene.getGeneId(), gene.getTaxon() ) ).thenReturn( gene );
        userGeneService.updateUserGenes();
        verify( userGeneRepository ).save( userGene );
    }
}
