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
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

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

    @MockBean
    MessageSource messageSource;

    @Before
    public void setUp() {
        when( siteSettings.getAdminEmail() ).thenReturn( "admin@example.com" );
        when( siteSettings.getFullUrl() ).thenReturn( URI.create( "http://localhost" ) );
    }

    @Test
    public void sendResetTokenMessage_thenSucceed() throws MessagingException {
        when( siteSettings.getFullUrl() ).thenReturn( URI.create( "http://localhost" ) );
        User user = createUser( 1 );
        emailService.sendResetTokenMessage( "1234", user );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "admin@example.com" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ user.getEmail() } );
        assertThat( mailMessageCaptor.getValue().getText() )
                .contains( "http://localhost/updatePassword?id=1&token=1234" );
    }

    @Test
    public void sendRegistrationMessageMessage_thenSucceed() {
        User user = createUser( 1 );
        emailService.sendRegistrationMessage( user, "1234" );
        ArgumentCaptor<SimpleMailMessage> mailMessageCaptor = ArgumentCaptor.forClass( SimpleMailMessage.class );
        verify( emailSender ).send( mailMessageCaptor.capture() );
        assertThat( mailMessageCaptor.getValue() )
                .hasFieldOrPropertyWithValue( "from", "admin@example.com" )
                .hasFieldOrPropertyWithValue( "to", new String[]{ user.getEmail() } );
        assertThat( mailMessageCaptor.getValue().getText() ).
                contains( "http://localhost/registrationConfirm?token=1234" );
    }
}
