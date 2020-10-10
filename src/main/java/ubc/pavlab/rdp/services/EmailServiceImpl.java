package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Created by mjacobson on 19/01/18.
 */
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private SiteSettings siteSettings;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    MessageSource messageSource;

    private void sendSimpleMessage( String subject, String content, String to, String replyTo ) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setSubject( subject );
        email.setText( content );
        email.setTo( to );
        email.setFrom( siteSettings.getAdminEmail() );
        if ( replyTo != null ) {
            email.setReplyTo( replyTo );
        }

        emailSender.send( email );

    }

    private void sendMultipartMessage( String subject, String content, String to, String replyTo, MultipartFile attachment ) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper( message, true );

        helper.setSubject( subject );
        helper.setText( content );
        helper.setTo( to );
        helper.setFrom( siteSettings.getAdminEmail() );
        if ( replyTo != null ) {
            helper.setReplyTo( replyTo );
        }

        helper.addAttachment( attachment.getOriginalFilename(), attachment );

        emailSender.send( message );
    }

    @Override
    public void sendSupportMessage( String message, String name, User user, HttpServletRequest request, MultipartFile attachment ) throws MessagingException {
        String replyTo = user.getProfile().getContactEmail() == null ? user.getEmail() : user.getProfile().getContactEmail();
        String content = "Name: " + name + "\r\n" +
                "Email: " + user.getEmail() + "\r\n" +
                "User-Agent: " + request.getHeader( "User-Agent" ) + "\r\n" +
                "Message: " + message + "\r\n" +
                "File Attached: " + ( attachment != null && !attachment.getOriginalFilename().equals( "" ) );

        if ( attachment == null ) {
            sendSimpleMessage( "Registry Help - Contact Support", content, siteSettings.getAdminEmail(), replyTo );
        } else {
            sendMultipartMessage( "Registry Help - Contact Support", content, siteSettings.getAdminEmail(), replyTo, attachment );
        }
    }

    @Override
    public void sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) {
        String url = UriComponentsBuilder.fromUri( siteSettings.getFullUrl() )
                .path( "updatePassword" )
                .queryParam( "id", user.getId() )
                .queryParam( "token", token.getToken() )
                .build().toUriString();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofLocalizedDateTime( FormatStyle.SHORT )
                .withLocale( locale )
                .withZone( ZoneId.systemDefault());

        String content =
                "Hello " + user.getProfile().getName() + ",\r\n\r\n" +
                        "We recently received a request that you want to reset your password. " +
                        "In order to reset your password, please click the confirmation link below:\r\n\r\n" +
                        url + "\r\n\r\n" +
                        "If you did not initiate this request, please disregard and delete this e-mail. " +
                        "Please note that this link will expire on " + dateTimeFormatter.format( token.getExpiryDate().toInstant() ) + ".";


        sendSimpleMessage( "Reset Password", content, user.getEmail(), null );
    }

    @Override
    public void sendRegistrationMessage( User user, VerificationToken token ) {
        String registrationWelcome = messageSource.getMessage( "rdp.site.email.registration-welcome", new String[]{ siteSettings.getFullUrl().toString() }, Locale.getDefault() );
        String registrationEnding = messageSource.getMessage( "rdp.site.email.registration-ending", new String[]{ siteSettings.getContactEmail() }, Locale.getDefault() );
        String recipientAddress = user.getEmail();
        String subject = "Registration Confirmation";
        String confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getFullUrl() )
                .path( "registrationConfirm" )
                .queryParam( "token", token.getToken() )
                .build()
                .toUriString();
        String message = registrationWelcome +
                "\r\n\r\nPlease confirm your registration by clicking on the following link:\r\n\r\n" +
                confirmationUrl +
                "\r\n\r\n" +
                registrationEnding;
        sendSimpleMessage( subject, message, recipientAddress, null );
    }

    @Override
    public void sendUserRegisteredEmail( User user ) {
        sendSimpleMessage( messageSource.getMessage( "rdp.site.shortname", null, Locale.getDefault() ) + " - User Registered", "New user registration: " + user.getEmail(), siteSettings.getAdminEmail(), null );
    }

}
