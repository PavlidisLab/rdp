package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

import static org.junit.Assume.assumeNoException;

public class Gene2GoParserIntegrationTest {

    /**
     * This test can be lengthy!
     */
    @Test
    public void parse_withOnlineFile_thenSucceeds() throws ParseException {
        Gene2GoParser parser = new Gene2GoParser( Collections.singleton( 9606 ) );
        try ( InputStream is = new GZIPInputStream( new UrlResource( "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene2go.gz" ).getInputStream() ) ) {
            parser.parse( is );
        } catch ( IOException e ) {
            assumeNoException( e );
        }
    }

    @Test
    public void parse_withOnlineFile_whenFileIsEmpty_thenSkipTheWholeFile() throws ParseException {
        Gene2GoParser parser = new Gene2GoParser( Collections.emptySet() );
        try ( InputStream is = new GZIPInputStream( new UrlResource( "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene2go.gz" ).getInputStream() ) ) {
            parser.parse( is );
        } catch ( IOException e ) {
            assumeNoException( e );
        }
    }
}