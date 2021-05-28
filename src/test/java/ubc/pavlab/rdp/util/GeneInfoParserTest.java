package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    public void parse_whenUrlFtp_thenSucceed() throws IOException, ParseException {
        List<GeneInfoParser.Record> genes = geneInfoParser.parse( new GZIPInputStream( new UrlResource( new URL( "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz" ) ).getInputStream() ) );
        assertThat( genes ).isNotEmpty();
    }
}
