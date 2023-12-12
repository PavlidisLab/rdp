package ubc.pavlab.rdp.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.PathResource;
import org.springframework.validation.Errors;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class EmailValidatorTest {

    private EmailValidator v;
    private Errors e;

    @BeforeEach
    public void setUp() {
        v = new EmailValidator();
        e = mock( Errors.class );
    }

    @Test
    public void validate_whenDomainIsAllowed_thenAccept() {
        v.validate( "test@test.com", e );
        verifyNoInteractions( e );
    }

    @Test
    public void validate_whenDomainIsNotInAllowedDomains_thenReject() {
        v = new EmailValidator( Collections.singleton( "test.com" ), false );
        v.validate( "test@test2.com", e );
        verify( e ).rejectValue( null, "EmailValidator.domainNotAllowed", new String[]{ "test2.com" }, null );
    }

    @Test
    public void validate_whenIdnIsEnabledAndDomainHasUnicodeSymbols_thenAccept() {
        v = new EmailValidator( (AllowedDomainStrategy) null, true );
        v.validate( "foo@Bücher.example", e );
        verifyNoInteractions( e );
    }

    @Test
    public void validate_whenDomainContainsUnsupportedCharacters_thenReject() {
        v.validate( "foo@Bücher.example", e );
        verify( e ).rejectValue( null, "EmailValidator.domainContainsUnsupportedCharacters" );
    }

    @Test
    public void validate_whenDomainIsMissing_thenReject() {
        v.validate( "test", e );
        verify( e ).rejectValue( null, "EmailValidator.invalidAddress" );
    }

    @Test
    public void validate_whenDomainIsEmpty_thenReject() {
        v.validate( "test@", e );
        verify( e ).rejectValue( null, "EmailValidator.emptyDomain" );
    }

    @Test
    public void validate_whenAddressIsEmpty_thenReject() {
        v.validate( "@test.com", e );
        verify( e ).rejectValue( null, "EmailValidator.emptyUser" );
    }

    @RepeatedTest(10)
    public void validate_whenDelayForRefreshingExpiresAndDomainIsRemoved_thenReject() throws Exception {
        Path tmpFile = Files.createTempFile( "test", null );

        try ( BufferedWriter writer = Files.newBufferedWriter( tmpFile ) ) {
            writer.write( "ubc.ca" );
        }

        EmailValidator v = new EmailValidator( new ResourceBasedAllowedDomainStrategy( new PathResource( tmpFile ), Duration.ofMillis( 50 ) ), false );

        Errors errors = mock( Errors.class );
        v.validate( "foo@ubc.ca", errors );
        verifyNoInteractions( errors );

        try ( BufferedWriter writer = Files.newBufferedWriter( tmpFile ) ) {
            writer.write( "ubc2.ca" );
        }

        // no immediate change
        errors = mock( Errors.class );
        v.validate( "foo@ubc.ca", errors );
        verifyNoInteractions( errors );

        // until the refresh delay expires...
        Thread.sleep( 50 );

        errors = mock( Errors.class );
        v.validate( "foo@ubc.ca", errors );
        verify( errors ).rejectValue( null, "EmailValidator.domainNotAllowed", new String[]{ "ubc.ca" }, null );

        errors = mock( Errors.class );
        v.validate( "foo@ubc2.ca", errors );
        verifyNoInteractions( errors );
    }
}