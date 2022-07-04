package ubc.pavlab.rdp.services;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

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
    private UserService userService;

    @MockBean
    private TierService tierService;
    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private ApplicationSettings applicationSettings;

    @MockBean
    private OntologyService ontologyService;

    @Test
    public void updateUserGenes_withExistingUser_thenUserGeneAreUpdated() {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo gene = createGene( 1, humanTaxon );
        UserGene userGene = createUserGene( 1, gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        userGene.setGeneInfo( gene );
        when( userGeneRepository.findAllWithGeneInfo() ).thenReturn( Lists.newArrayList( userGene ) );
        when( geneInfoRepository.findByGeneIdAndTaxon( gene.getGeneId(), gene.getTaxon() ) ).thenReturn( gene );
        userGeneService.updateUserGenes();
        verify( userGeneRepository ).save( userGene );
    }
}
