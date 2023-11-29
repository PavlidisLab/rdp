package ubc.pavlab.rdp.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.PathResource;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.Errors;
import ubc.pavlab.rdp.ValidationConfig;
import ubc.pavlab.rdp.validation.EmailValidator;
import ubc.pavlab.rdp.validation.ResourceBasedAllowedDomainStrategy;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "rdp.settings.allowed-email-domains-file=classpath:allowed-email-domains-test.txt",
        "rdp.settings.allowed-email-domains-refresh-delay=PT1S",
        "rdp.settings.allow-internationalized-domain-names=true"
})
public class EmailValidatorFactoryTest {

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
    public void test() throws Exception {
        Errors errors = mock( Errors.class );
        emailValidator.validate( "foo@ubc.ca", errors );
        verifyNoInteractions( errors );
    }

    @Test
    public void testUnrecognizedDomain() throws Exception {
        Errors errors = mock( Errors.class );
        emailValidator.validate( "foo@ubc2.ca", errors );
        verify( errors ).reject( "EmailValidator.domainNotAllowed" );
    }

    @Test
    public void testReloadAfterDelay() throws Exception {
        Path tmpFile = Files.createTempFile( "test", null );

        try ( BufferedWriter writer = Files.newBufferedWriter( tmpFile ) ) {
            writer.write( "ubc.ca" );
        }

        EmailValidator v = new EmailValidator( new ResourceBasedAllowedDomainStrategy( new PathResource( tmpFile ), Duration.ofMillis( 100 ) ), false );

        Errors errors = mock( Errors.class );
        v.validate( "foo@ubc.ca", errors );
        verifyNoInteractions( errors );

        try ( BufferedWriter writer = Files.newBufferedWriter( tmpFile ) ) {
            // clearing the file
        }

        // no immediate change
        errors = mock( Errors.class );
        v.validate( "foo@ubc.ca", errors );
        verifyNoInteractions( errors );

        // until the refresh delay expires...
        Thread.sleep( 100 );

        errors = mock( Errors.class );
        v.validate( "foo@ubc.ca", errors );
        verify( errors ).reject( "EmailValidator.domainNotAllowed" );
        assertNotSame( v, emailValidator );
    }
}