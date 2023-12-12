package ubc.pavlab.rdp.validation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.client.RestTemplate;

/**
 * reCAPTCHA v2 implementation as a Spring validator.
 *
 * @author poirigui
 */
public class RecaptchaValidator implements Validator {
    private final RestTemplate restTemplate;
    private final String secret;

    public RecaptchaValidator( RestTemplate restTemplate, String secret ) {
        Assert.isTrue( restTemplate.getMessageConverters().stream().anyMatch( converter -> converter.canWrite( MultiValueMap.class, MediaType.APPLICATION_FORM_URLENCODED ) ),
                "The supplied RestTemplate must support writing " + MediaType.APPLICATION_FORM_URLENCODED_VALUE + " messages." );
        Assert.isTrue( StringUtils.isNotBlank( secret ), "The secret must not be empty." );
        this.restTemplate = restTemplate;
        this.secret = secret;
    }

    @Override
    public void validate( Object target, Errors errors ) {
        Recaptcha recaptcha = (Recaptcha) target;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType( MediaType.APPLICATION_FORM_URLENCODED );
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add( "secret", secret );
        payload.add( "response", recaptcha.getResponse() );
        if ( recaptcha.getRemoteIp() != null ) {
            payload.add( "remoteip", recaptcha.getRemoteIp() );
        }
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>( payload, headers );
        Reply reply = restTemplate.postForObject( "https://www.google.com/recaptcha/api/siteverify",
                requestEntity, Reply.class );
        if ( reply == null ) {
            errors.reject( "RecaptchaValidator.empty-reply" );
            return;
        }
        if ( !reply.success ) {
            errors.reject( "RecaptchaValidator.unsuccessful-response" );
        }
        if ( reply.errorCodes != null ) {
            for ( String errorCode : reply.errorCodes ) {
                errors.reject( "RecaptchaValidator." + errorCode );
            }
        }
    }

    @Override
    public boolean supports( Class<?> clazz ) {
        return Recaptcha.class.isAssignableFrom( clazz );
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    private static class Reply {
        private boolean success;
        private String challengeTs;
        private String hostname;
        @Nullable
        private String[] errorCodes;
    }
}
