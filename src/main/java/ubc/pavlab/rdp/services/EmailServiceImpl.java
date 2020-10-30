package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
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
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Created by mjacobson on 19/01/18.
 */
@Service
@CommonsLog
public class EmailServiceImpl implements EmailService {

    @Autowired
    private SiteSettings siteSettings;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    MessageSource messageSource;

    private void sendSimpleMessage( String subject, String content, InternetAddress to, InternetAddress replyTo ) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setSubject( subject );
        email.setText( content );
        email.setTo( to.toString() );
        email.setFrom( siteSettings.getAdminEmail() );
        if ( replyTo != null ) {
            email.setReplyTo( replyTo.toString() );
        }

        emailSender.send( email );

    }

    private void sendMultipartMessage( String subject, String content, InternetAddress to, InternetAddress replyTo, MultipartFile attachment ) throws MessagingException {
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
        InternetAddress replyTo = user.getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        String content = "Name: " + name + "\r\n" +
                "Email: " + user.getEmail() + "\r\n" +
                "User-Agent: " + request.getHeader( "User-Agent" ) + "\r\n" +
                "Message: " + message + "\r\n" +
                "File Attached: " + ( attachment != null && !attachment.getOriginalFilename().equals( "" ) );

        if ( attachment == null ) {
            sendSimpleMessage( "Registry Help - Contact Support", content, new InternetAddress( siteSettings.getAdminEmail() ), replyTo );
        } else {
            sendMultipartMessage( "Registry Help - Contact Support", content, new InternetAddress( siteSettings.getAdminEmail() ), replyTo, attachment );
        }
    }

    @Override
    public void sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException {
        String url = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "updatePassword" )
                .queryParam( "id", user.getId() )
                .queryParam( "token", token.getToken() )
                .build().toUriString();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofLocalizedDateTime( FormatStyle.SHORT )
                .withLocale( locale )
                .withZone( ZoneId.systemDefault() );

        // password reset always go through the primary email
        InternetAddress to = new InternetAddress( user.getEmail() );
        String subject = "Reset your password";
        String content =
                "Hello " + user.getProfile().getName() + ",\r\n\r\n" +
                        "We recently received a request that you want to reset your password. " +
                        "In order to reset your password, please click the confirmation link below:\r\n\r\n" +
                        url + "\r\n\r\n" +
                        "If you did not initiate this request, please disregard and delete this e-mail. " +
                        "Please note that this link will expire on " + dateTimeFormatter.format( token.getExpiryDate().toInstant() ) + ".";


        sendSimpleMessage( subject, content, to, null );
    }

    @Override
    public void sendRegistrationMessage( User user, VerificationToken token ) throws MessagingException {
        String registrationWelcome = messageSource.getMessage( "rdp.site.email.registration-welcome", new String[]{ siteSettings.getHostUri().toString() }, Locale.getDefault() );
        String registrationEnding = messageSource.getMessage( "rdp.site.email.registration-ending", new String[]{ siteSettings.getContactEmail() }, Locale.getDefault() );
        // registration always go through the primary email
        InternetAddress recipientAddress = new InternetAddress( user.getEmail() );
        String subject = "Confirm your registration";
        String confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
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
    public void sendContactEmailVerificationMessage( User user, VerificationToken token ) throws MessagingException {
        InternetAddress recipientAddress = new InternetAddress( user.getProfile().getContactEmail() );
        String subject = "Verify your contact email";
        String confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "user/verify-contact-email" )
                .queryParam( "token", token.getToken() )
                .build()
                .toUriString();
        String message = MessageFormat.format( "Please verify your contact email by clicking on the following link:\r\n\r\n{0}",
                confirmationUrl );
        sendSimpleMessage( subject, message, recipientAddress, null );
    }

    @Override
    public void sendUserRegisteredEmail( User user ) throws MessagingException {
        sendSimpleMessage( messageSource.getMessage( "rdp.site.shortname", null, Locale.getDefault() ) + " - User Registered", "New user registration: " + user.getEmail(), new InternetAddress( siteSettings.getAdminEmail() ), null );
    }

}
