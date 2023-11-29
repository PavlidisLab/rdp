package ubc.pavlab.rdp.validation;

import org.junit.Test;
import org.springframework.core.io.UrlResource;

import java.io.IOException;

public class ResourceBasedAllowedDomainStrategyIntegrationTest {

    @Test
    public void testWithJetBrainsSwot() throws IOException {
        ResourceBasedAllowedDomainStrategy strategy = new ResourceBasedAllowedDomainStrategy( new UrlResource( "https://github.com/JetBrains/swot/releases/download/latest/swot.txt" ), null );
        strategy.refresh();
    }
}