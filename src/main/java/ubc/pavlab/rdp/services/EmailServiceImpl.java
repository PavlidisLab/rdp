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
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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
    private MessageSource messageSource;

    private Future<Void> sendSimpleMessage( String subject, String content, InternetAddress to, InternetAddress replyTo, InternetAddress cc ) throws AddressException {
        SimpleMailMessage email = new SimpleMailMessage();

        email.setSubject( subject );
        email.setText( content );
        email.setTo( to.toString() );
        email.setFrom( getAdminAddress().toString() );
        if ( replyTo != null ) {
            email.setReplyTo( replyTo.toString() );
        }
        if ( cc != null ) {
            email.setCc( cc.toString() );
        }

        return CompletableFuture.runAsync( () -> emailSender.send( email ) );
    }

    private Future<Void> sendMultipartMessage( String subject, String content, InternetAddress to, InternetAddress replyTo, MultipartFile attachment ) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper( message, true );

        helper.setSubject( subject );
        helper.setText( content );
        helper.setTo( to );
        helper.setFrom( getAdminAddress() );
        if ( replyTo != null ) {
            helper.setReplyTo( replyTo );
        }

        helper.addAttachment( attachment.getOriginalFilename(), attachment );

        return CompletableFuture.runAsync( () -> emailSender.send( message ) );
    }

    @Override
    public Future<Void> sendSupportMessage( String message, String name, User user, String userAgent, MultipartFile attachment, Locale locale ) throws MessagingException {
        InternetAddress replyTo = user.getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        String shortName = messageSource.getMessage( "rdp.site.shortname", new String[]{ siteSettings.getHostUri().toString() }, Locale.getDefault() );
        String subject = messageSource.getMessage( "EmailService.sendSupportMessage.subject", new String[]{ shortName }, locale );
        String content = "Name: " + name + "\r\n" +
                "Email: " + user.getEmail() + "\r\n" +
                "User-Agent: " + userAgent + "\r\n" +
                "Message: " + message + "\r\n" +
                "File Attached: " + ( attachment != null && !attachment.getOriginalFilename().equals( "" ) );
        if ( attachment == null ) {
            return sendSimpleMessage( subject, content, getAdminAddress(), replyTo, null );
        } else {
            return sendMultipartMessage( subject, content, getAdminAddress(), replyTo, attachment );
        }
    }

    @Override
    public Future<Void> sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException {
        URI url = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "updatePassword" )
                .queryParam( "id", "{id}" )
                .queryParam( "token", "{token}" )
                .build( user.getId(), token.getToken() );

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofLocalizedDateTime( FormatStyle.SHORT )
                .withLocale( locale )
                .withZone( ZoneId.systemDefault() );

        // password reset always go through the primary email
        InternetAddress to = new InternetAddress( user.getEmail() );
        String shortName = messageSource.getMessage( "rdp.site.shortname", new String[]{ siteSettings.getHostUri().toString() }, locale );
        String subject = messageSource.getMessage( "EmailService.sendResetTokenMessage.subject", new String[]{ shortName }, locale );
        String content = messageSource.getMessage( "EmailService.sendResetTokenMessage", new String[]{
                user.getProfile().getName(), url.toString(), dateTimeFormatter.format( token.getExpiryDate().toInstant() ) }, locale );

        return sendSimpleMessage( subject, content, to, null, null );
    }

    @Override
    public Future<Void> sendRegistrationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException {
        String shortName = messageSource.getMessage( "rdp.site.shortname", new String[]{ siteSettings.getHostUri().toString() }, locale );
        String registrationWelcome = messageSource.getMessage( "rdp.site.email.registration-welcome", new String[]{ siteSettings.getHostUri().toString(), shortName }, locale );
        String registrationEnding = messageSource.getMessage( "rdp.site.email.registration-ending", new String[]{ siteSettings.getContactEmail() }, locale );
        // registration always go through the primary email
        InternetAddress recipientAddress = new InternetAddress( user.getEmail() );
        String subject = messageSource.getMessage( "EmailService.sendRegistrationMessage.subject", new String[]{ shortName }, locale );
        URI confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "registrationConfirm" )
                .queryParam( "token", "{token}" )
                .build( token.getToken() );
        String message = registrationWelcome + "\r\n\r\n" +
                messageSource.getMessage( "EmailService.sendRegistrationMessage", new String[]{ confirmationUrl.toString() }, locale ) + "\r\n\r\n" +
                registrationEnding;
        return sendSimpleMessage( subject, message, recipientAddress, null, null );
    }

    @Override
    public Future<Void> sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException {
        InternetAddress recipientAddress = new InternetAddress( user.getProfile().getContactEmail() );
        String shortName = messageSource.getMessage( "rdp.site.shortname", new String[]{ siteSettings.getHostUri().toString() }, locale );
        String subject = messageSource.getMessage( "EmailService.sendContactEmailVerificationMessage.subject", new String[]{ shortName }, locale );
        String confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "user/verify-contact-email" )
                .queryParam( "token", token.getToken() )
                .build()
                .toUriString();
        String message = messageSource.getMessage( "EmailService.sendContactEmailVerificationMessage", new String[]{ confirmationUrl }, locale );
        return sendSimpleMessage( subject, message, recipientAddress, null, null );
    }

    @Override
    public Future<Void> sendUserRegisteredEmail( User user ) throws MessagingException {
        // unfortunately, there's no way to tell the dmin locale
        Locale locale = Locale.getDefault();
        String shortname = messageSource.getMessage( "rdp.site.shortname", null, locale );
        String subject = messageSource.getMessage( "EmailService.sendUserRegisteredEmail.subject", new String[]{ shortname }, locale );
        String content = messageSource.getMessage( "EmailService.sendUserRegisteredEmail", new String[]{ user.getEmail() }, locale );
        return sendSimpleMessage( subject, content, getAdminAddress(), null, null );
    }

    @Override
    public Future<Void> sendUserGeneAccessRequest( UserGene userGene, User replyTo, String reason ) throws MessagingException {
        URI viewUserUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUri() )
                .path( "userView/{userId}" )
                .build( Collections.singletonMap( "userId", replyTo.getId() ) );
        InternetAddress to = userGene.getUser().getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        InternetAddress replyToAddress = replyTo.getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        // unfortunately, there's no way to tell the recipient locale for now
        Locale locale = Locale.getDefault();
        String shortname = messageSource.getMessage( "rdp.site.shortname", null, locale );
        String subject = messageSource.getMessage( "EmailService.sendUserGeneAccessRequest.subject", new String[]{ shortname }, locale );
        String content = messageSource.getMessage( "EmailService.sendUserGeneAccessRequest",
                new String[]{ replyTo.getProfile().getFullName(), userGene.getSymbol(), reason, viewUserUrl.toString() }, locale );
        return sendSimpleMessage( subject, content, to, replyToAddress, getAdminAddress() );
    }

    private InternetAddress getAdminAddress() throws AddressException {
        try {
            return new InternetAddress( siteSettings.getAdminEmail(), messageSource.getMessage( "rdp.site.shortname", null, Locale.getDefault() ) );
        } catch ( UnsupportedEncodingException e ) {
            log.error( "Could not encode the admin email personal, please set rdp.site.shortname correctly.", e );
            return new InternetAddress( siteSettings.getAdminEmail() );
        }
    }
}
