package ubc.pavlab.rdp.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.Errors;
import ubc.pavlab.rdp.ValidationConfig;
import ubc.pavlab.rdp.validation.EmailValidator;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "rdp.settings.allowed-email-domains-file=classpath:allowed-email-domains-test.txt",
        "rdp.settings.allowed-email-domains-refresh-delay=PT0.1S",
        "rdp.settings.allow-internationalized-domain-names=true"
})
public class EmailValidatorWithContextTest {

    @TestConfiguration
    @Import(ValidationConfig.class)
    static class EmailValidatorFactoryTestContextConfiguration {

        @Bean
        public ConversionService conversionService() {
            return new DefaultFormattingConversionService();
        }
    }

    @Autowired
    private EmailValidator emailValidator;

    @Test
    public void test() {
        Errors errors = mock( Errors.class );
        emailValidator.validate( "foo@ubc.ca", errors );
        verifyNoInteractions( errors );
    }

    @Test
    public void testUnrecognizedDomain() {
        Errors errors = mock( Errors.class );
        emailValidator.validate( "foo@ubc2.ca", errors );
        verify( errors ).rejectValue( null, "EmailValidator.domainNotAllowed", new String[]{ "ubc2.ca" }, null );
    }
}