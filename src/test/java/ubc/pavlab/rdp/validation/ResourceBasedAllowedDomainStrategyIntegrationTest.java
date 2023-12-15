package ubc.pavlab.rdp.validation;

import org.junit.Test;
import org.springframework.core.io.UrlResource;

import java.io.IOException;

import static org.assertj.core.api.Assumptions.assumeThat;

public class ResourceBasedAllowedDomainStrategyIntegrationTest {

    @Test
    public void testWithJetBrainsSwot() throws IOException {
        UrlResource resource = new UrlResource( "https://github.com/JetBrains/swot/releases/download/latest/swot.txt" );
        assumeThat( resource.exists() ).isTrue();
        ResourceBasedAllowedDomainStrategy strategy = new ResourceBasedAllowedDomainStrategy( resource, null );
        strategy.refresh();
    }
}