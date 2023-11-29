package ubc.pavlab.rdp.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.validation.Errors;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class EmailValidatorTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Errors e;

    private EmailValidator v;

    @Before
    public void setUp() {
        v = new EmailValidator();
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
        verify( e ).rejectValue( null, "EmailValidator.domainNotAllowed" );
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
}