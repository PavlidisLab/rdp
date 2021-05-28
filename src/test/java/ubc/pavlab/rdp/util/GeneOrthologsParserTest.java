package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(SpringRunner.class)
public class GeneOrthologsParserTest {

    @TestConfiguration
    public static class GeneOrthologsParserTestContextConfiguration {

        @Bean
        public GeneOrthologsParser geneOrthologsParser() {
            return new GeneOrthologsParser();
        }
    }

    @Autowired
    private GeneOrthologsParser geneOrthologsParser;

    @Test
    public void parse_thenSucceeed() throws IOException, ParseException {
        List<GeneOrthologsParser.Record> records = geneOrthologsParser.parse( new GZIPInputStream( new ClassPathResource( "cache/gene_orthologs.gz" ).getInputStream() ) );
        assertThat( records ).isNotEmpty();
    }

    @Test
    public void parse_whenDIOPT_thenSucceeed() throws IOException, ParseException {
        List<GeneOrthologsParser.Record> records = geneOrthologsParser.parse( new GZIPInputStream( new UrlResource( "file:DIOPT_v8.tsv.gz" ).getInputStream() ) );
        assertThat( records ).isNotEmpty();
    }
}
