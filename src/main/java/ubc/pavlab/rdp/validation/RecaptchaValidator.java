package ubc.pavlab.rdp.validation;

import lombok.Data;
import lombok.Value;
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
        this.restTemplate = restTemplate;
        this.secret = secret;
    }

    @Override
    public void validate( Object target, Errors errors ) {
        Recaptcha recaptcha = (Recaptcha) target;
        Reply reply = restTemplate.postForObject( "https://www.google.com/recaptcha/api/siteverify",
                new Payload( secret, recaptcha.getResponse(), recaptcha.getRemoteIp() ), Reply.class );
        if ( reply == null ) {
            errors.reject( "" );
            return;
        }
        if ( !reply.success ) {
            errors.reject( "" );
        }
        for ( String errorCode : reply.errorCodes ) {
            errors.reject( errorCode );
        }
    }

    @Override
    public boolean supports( Class<?> clazz ) {
        return Recaptcha.class.isAssignableFrom( clazz );
    }

    @Value
    private static class Payload {
        String secret;
        String response;
        String remoteIp;
    }

    @Data
    private static class Reply {
        private boolean success;
        private String challengeTs;
        private String hostname;
        private String[] errorCodes;
    }
}
