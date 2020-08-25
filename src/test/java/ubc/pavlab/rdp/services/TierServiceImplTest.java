package ubc.pavlab.rdp.services;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.util.TestUtils;

@RunWith(SpringRunner.class)
public class TierServiceImplTest {

    @TestConfiguration
    static class TierServiceImplTestContextConfiguration {

        @Bean
        public TierService tierService() {
            return new TierServiceImpl();
        }
    }

    @MockBean
    private PermissionEvaluator permissionEvaluator;
}
