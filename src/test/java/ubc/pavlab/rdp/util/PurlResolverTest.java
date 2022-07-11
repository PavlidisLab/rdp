package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;

@Import(PurlResolver.class)
@RunWith(SpringRunner.class)
public class PurlResolverTest {

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void test() {
        assertThat( resourceLoader ).isInstanceOf( DefaultResourceLoader.class );
        assertThat( ( (DefaultResourceLoader) resourceLoader ).getProtocolResolvers() )
                .hasSize( 1 )
                .first()
                .isInstanceOf( PurlResolver.class );
    }

    @Test
    public void getResource() {
        Resource resource = resourceLoader.getResource( "http://purl.obolibrary.org/obo/uberon.obo" );
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( resource.getInputStream() ) ) ) {
            assertThat( reader.readLine() ).startsWith( "format-version:" );
        } catch ( IOException e ) {
            assumeNoException( "Network is likely unavailable for reaching purl.obolibrary.org.", e );
        }
    }

    @Test
    public void getResource_withRegularResourceLoader_thenFailToResolveUberon() {
        Resource resource = new DefaultResourceLoader().getResource( "http://purl.obolibrary.org/obo/uberon.obo" );
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( resource.getInputStream() ) ) ) {
            assertThat( reader.readLine() ).doesNotStartWith( "format-version:" );
        } catch ( IOException e ) {
            assumeNoException( "Network is likely unavailable for reaching purl.obolibrary.org.", e );
        }
    }

}