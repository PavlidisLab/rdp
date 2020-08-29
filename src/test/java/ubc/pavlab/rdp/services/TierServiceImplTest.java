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
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class TierServiceImplTest {

    @TestConfiguration
    static class TierServiceImplTestContextConfiguration {

        @Bean
        public TierService tierService() {
            return new TierServiceImpl();
        }

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings as = new ApplicationSettings();
            as.setEnabledTiers( Lists.newArrayList("TIER1", "TIER2", "TIER3") );
            return as;
        }
    }

    @Autowired
    TierService tierService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @Test
    public void isTierEnabled() {
        assertThat(tierService.isTierEnabled( TierType.TIER1 )).isTrue();
        assertThat(tierService.isTierEnabled( TierType.TIER2 )).isTrue();
        assertThat(tierService.isTierEnabled( TierType.TIER3 )).isTrue();
    }
}
