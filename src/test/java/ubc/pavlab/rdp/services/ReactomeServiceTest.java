package ubc.pavlab.rdp.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import ubc.pavlab.rdp.RemoteResourceConfig;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@Import(RemoteResourceConfig.class)
@DataJpaTest
public class ReactomeServiceTest {

    @TestConfiguration
    public static class OntologyServiceTestContextConfiguration {

        @Bean
        public OntologyService ontologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository ) {
            return new OntologyService( ontologyRepository, ontologyTermInfoRepository );
        }

        @Bean
        public ReactomeService reactomeService( OntologyService ontologyService, ApplicationSettings applicationSettings, AsyncRestTemplate asyncRestTemplate ) {
            return new ReactomeService( ontologyService, applicationSettings, asyncRestTemplate );
        }
    }

    @Autowired
    private ReactomeService reactomeService;

    @Autowired
    private OntologyService ontologyService;

    @MockBean
    private ApplicationSettings applicationSettings;

    @MockBean
    private ApplicationSettings.OntologySettings ontologySettings;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @MockBean(name = "messageSourceWithoutOntology")
    private MessageSource messageSource;

    /* fixtures */
    private Ontology reactome;

    @Before
    public void setUp() throws IOException, ReactomeException {
        when( applicationSettings.getOntology() ).thenReturn( ontologySettings );
        when( ontologySettings.getReactomePathwaysOntologyName() ).thenReturn( "reactome" );
        when( ontologySettings.getReactomePathwaysFile() ).thenReturn( new ClassPathResource( "cache/ReactomePathways.txt" ) );
        when( ontologySettings.getReactomePathwaysHierarchyFile() ).thenReturn( new ClassPathResource( "cache/ReactomePathwaysRelation.txt" ) );
        when( ontologySettings.getReactomeStableIdentifiersFile() ).thenReturn( new ClassPathResource( "cache/reactome_stable_ids.txt" ) );
        reactome = reactomeService.importPathwaysOntology();
        assertThat( ontologyService.activate( reactome, true ) ).isEqualTo( 2580 );
    }

    @Test
    public void importReactomePathways() {
        assertThat( reactome.getTerms() ).hasSize( 2580 );
        // the TSV does not have a header, so we must ensure that the first record is kept
        assertThat( ontologyService.findTermByTermIdAndOntology( "R-HSA-164843", reactome ) ).isNotNull();
        assertThat( ontologyService.autocompleteTerms( "R-HSA-164843", 10, Locale.getDefault() ) ).hasSize( 1 );
    }

    @Test
    public void updateReactomePathways() throws ReactomeException {
        reactomeService.updatePathwaysOntology();
    }

    @Test
    public void updateSummations() throws ReactomeException {
        when( applicationSettings.getOntology().getReactomeContentServiceUrl() )
                .thenReturn( URI.create( "https://reactome.org/ContentService" ) );
        JSONArray fakePayload = new JSONArray()
                .put( 0, new JSONObject()
                        .put( "stId", "R-HSA-164843" )
                        .put( "summation", new JSONArray()
                                .put( 0, new JSONObject()
                                        .put( "text", "the new summation" ) ) ) );

        // fake Reactome server
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( ExpectedCount.manyTimes(), requestTo( "https://reactome.org/ContentService/data/query/ids" ) )
                .andExpect( method( HttpMethod.POST ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( fakePayload.toString() ) );

        reactomeService.updatePathwaySummations( null );

        assertThat( ontologyService.findTermByTermIdAndOntology( "R-HSA-164843", reactome ) )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "definition", "the new summation" );

        mockServer.verify();
    }
}
