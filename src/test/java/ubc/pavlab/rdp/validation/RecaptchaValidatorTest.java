package ubc.pavlab.rdp.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@JsonTest
public class RecaptchaValidatorTest {

    private RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void test() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( restTemplate );
        MultiValueMap<String, String> expectedFormData = new LinkedMultiValueMap<>();
        expectedFormData.add( "secret", "1234" );
        expectedFormData.add( "response", "I'm human." );
        expectedFormData.add( "remoteip", "127.0.0.1" );
        mockServer.expect( requestTo( "https://www.google.com/recaptcha/api/siteverify" ) )
                .andExpect( content().formData( expectedFormData ) )
                .andRespond( withStatus( HttpStatus.OK ).contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new Reply( true, "", "localhost", null ) ) ) );
        Validator validator = new RecaptchaValidator( restTemplate, "1234" );
        Recaptcha recaptcha = new Recaptcha( "I'm human.", "127.0.0.1" );
        Errors errors = new BeanPropertyBindingResult( recaptcha, "recaptcha" );
        validator.validate( recaptcha, errors );
        assertThat( errors.hasErrors() ).withFailMessage( errors.toString() ).isFalse();
        mockServer.verify();
    }

    @Test
    public void testInvalidRecaptchaResponse() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( restTemplate );
        MultiValueMap<String, String> expectedFormData = new LinkedMultiValueMap<>();
        expectedFormData.add( "secret", "1234" );
        expectedFormData.add( "response", "I'm a robot." );
        expectedFormData.add( "remoteip", "127.0.0.1" );
        mockServer.expect( requestTo( "https://www.google.com/recaptcha/api/siteverify" ) )
                .andExpect( content().formData( expectedFormData ) )
                .andRespond( withStatus( HttpStatus.OK ).contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new Reply( false, "", "localhost", new String[]{
                                "invalid-input-secret"
                        } ) ) ) );
        Validator validator = new RecaptchaValidator( restTemplate, "1234" );
        Recaptcha recaptcha = new Recaptcha( "I'm a robot.", "127.0.0.1" );
        Errors errors = new BeanPropertyBindingResult( recaptcha, "recaptcha" );
        validator.validate( recaptcha, errors );
        assertThat( errors.hasErrors() ).isTrue();
        assertThat( errors.getGlobalErrors() )
                .satisfiesExactlyInAnyOrder( ( f ) -> {
                    assertThat( f.getCode() ).isEqualTo( "RecaptchaValidator.unsuccessful-response" );
                }, ( f ) -> {
                    assertThat( f.getCode() ).isEqualTo( "RecaptchaValidator.invalid-input-secret" );
                } );
        mockServer.verify();
    }

    @Value
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    private static class Reply {
        boolean success;
        String challengeTs;
        String hostname;
        @Nullable
        String[] errorCodes;
    }
}