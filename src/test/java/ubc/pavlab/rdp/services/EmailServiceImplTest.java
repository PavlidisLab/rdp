package ubc.pavlab.rdp.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.WebMvcConfig;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubc.pavlab.rdp.util.TestUtils.*;

@RunWith(SpringRunner.class)
@Import(WebMvcConfig.class)
public class EmailServiceImplTest {

    @TestConfiguration
    public static class EmailServiceImplTestContextConfiguration {

        @Bean
        public EmailService emailService() {
            return new EmailServiceImpl();
        }
    }

    @Autowired
    private EmailService emailService;

    @MockBean
    private SiteSettings siteSettings;

    @MockBean
    private JavaMailSender emailSender;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @Before
    public void setUp() {
        when( siteSettings.getAdminEmail() ).thenReturn( "admin@example.com" );
        when( siteSettings.getHostUrl() ).thenReturn( URI.create( "http://localhost" ) );
    }

    @Test
    public void sendUserRegistered_thenSucceed() throws MessagingException {
        User user = createUser( 1 );
        assertThat( emailService.sendUserRegisteredEmail( user ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ "RDP <admin@example.com>" } );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessageCaptor.getValue().getText() )
                .contains( user.getEmail() );
    }

    @Test
    public void sendSupportMessage_thenSucceed() throws MessagingException {
        User user = createUser( 1 );
        user.setEnabled( true );
        user.setEnabledAt( Instant.now() );
        assertThat( emailService.sendSupportMessage( "I need help!", "John Doe", user, "Google Chrome", null, Locale.getDefault() ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ "RDP <admin@example.com>" } )
                .hasFieldOrPropertyWithValue( "replyTo", String.format( "\"Wayne, Bruce\" <%s>", user.getEmail() ) );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessageCaptor.getValue().getText() )
                .contains( "John Doe" )
                .contains( user.getEmail() )
                .contains( "Google Chrome" )
                .contains( "I need help!" );
    }

    public void sendResetTokenMessage_thenSucceed() throws MessagingException {
        when( siteSettings.getHostUrl() ).thenReturn( URI.create( "http://localhost" ) );
        User user = createUser( 1 );
        PasswordResetToken token = createPasswordResetToken( user, "1234" );
        assertThat( emailService.sendResetTokenMessage( user, token, Locale.getDefault() ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ user.getEmail() } );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessageCaptor.getValue().getText() )
                .contains( "http://localhost/updatePassword?id=1&token=1234" );
    }

    @Test
    public void sendRegistrationMessageMessage_thenSucceed() throws MessagingException {
        User user = createUser( 1 );
        VerificationToken token = createVerificationToken( user, "1234" );
        assertThat( emailService.sendRegistrationMessage( user, token, Locale.getDefault() ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ user.getEmail() } );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessageCaptor.getValue().getText() ).
                contains( "http://localhost/registrationConfirm?token=1234" );
    }

    @Test
    public void sendContactEmailVerificationMessage_thenSucceed() throws MessagingException, ExecutionException, InterruptedException {
        User user = createUser( 1 );
        user.getProfile().setContactEmail( "foo@example.com" );
        VerificationToken token = createContactEmailVerificationToken( user, "1234" );
        assertThat( emailService.sendContactEmailVerificationMessage( user, token, Locale.getDefault() ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ "foo@example.com" } );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessageCaptor.getValue().getText() ).
                contains( "http://localhost/user/verify-contact-email?token=1234" );
    }

    @Test
    public void sendContactEmailVerificationMessage_whenTokenContainsInvalidCharacter_thenSucceed() throws MessagingException, ExecutionException, InterruptedException {
        User user = createUser( 1 );
        user.getProfile().setContactEmail( "foo@example.com" );
        VerificationToken token = createContactEmailVerificationToken( user, "1234+" );
        assertThat( emailService.sendContactEmailVerificationMessage( user, token, Locale.getDefault() ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ "foo@example.com" } );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessageCaptor.getValue().getText() ).
                contains( "http://localhost/user/verify-contact-email?token=1234%2B" );
    }

    @Test
    public void sendUserGeneAccessRequest_thenSucceed() throws MessagingException {
        User user = createUser( 1 );
        user.getProfile().setContactEmail( "foo@example.com" );
        user.getProfile().setContactEmailVerified( true );
        user.getProfile().setContactEmailVerifiedAt( Instant.now() );
        User user2 = createUser( 2 );
        user2.getProfile().setContactEmail( "bar@example.com" );
        user2.getProfile().setContactEmailVerified( true );
        user2.getProfile().setContactEmailVerifiedAt( Instant.now() );
        UserGene userGene = createUserGene( 1, createGene( 1, createTaxon( 1 ) ), user2, TierType.TIER1, PrivacyLevelType.PRIVATE );
        assertThat( emailService.sendUserGeneAccessRequest( userGene, user, "Because." ) )
                .succeedsWithin( 1, TimeUnit.SECONDS );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        SimpleMailMessage mailMessage = mailMessageCaptor.getValue();
        assertThat( mailMessage )
                .hasFieldOrPropertyWithValue( "from", "RDP <admin@example.com>" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ "\"Wayne, Bruce\" <bar@example.com>" } )
                .hasFieldOrPropertyWithValue( "replyTo", "\"Wayne, Bruce\" <foo@example.com>" )
                .hasFieldOrPropertyWithValue( "cc", new String[]{ "RDP <admin@example.com>" } );
        assertThat( mailMessageCaptor.getValue().getSubject() ).contains( "RDP" );
        assertThat( mailMessage.getText() )
                .contains( userGene.getSymbol() )
                .contains( user.getProfile().getFullName() )
                .contains( "http://localhost/search/user/1" );
    }
}
