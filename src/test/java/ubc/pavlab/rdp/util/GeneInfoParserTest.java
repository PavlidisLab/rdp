package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

@CommonsLog
@RunWith(SpringRunner.class)
public class GeneInfoParserTest {

    @TestConfiguration
    static class GeneInfoParserTestContextConfiguration {
        @Bean
        public GeneInfoParser geneInfoParser() {
            return new GeneInfoParser();
        }

    }

    @Autowired
    private GeneInfoParser geneInfoParser;

    @MockBean
    private FTPClient ftpClient;

    @Test
    public void parse_whenUrlFtp_thenSucceed() throws IOException, ParseException {
        when( ftpClient.retrieveFileStream( "/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz" ) )
                .thenReturn( new ClassPathResource( "cache/genes/Homo_sapiens.gene_info.gz" ).getInputStream() );
        List<GeneInfoParser.Record> genes = geneInfoParser.parse( new URL( "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz" ) );
        verify( ftpClient ).connect( "ftp.ncbi.nlm.nih.gov" );
        verify( ftpClient ).login( "anonymous", "" );
        verify( ftpClient ).setFileType( FTPClient.BINARY_FILE_TYPE );
        assertThat( genes ).isNotEmpty();
    }
}
