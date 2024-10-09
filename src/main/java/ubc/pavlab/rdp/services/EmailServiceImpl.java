package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.Messages;

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
import java.util.HashMap;
import java.util.Locale;
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

    @Autowired
    @Qualifier("emailTaskExecutor")
    private AsyncTaskExecutor executorService;

    private Future<?> sendSimpleMessage( String subject, String content, InternetAddress to, @Nullable InternetAddress replyTo, @Nullable InternetAddress cc ) throws AddressException {
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

        return executorService.submit( () -> emailSender.send( email ) );
    }

    private Future<?> sendMultipartMessage( String subject, String content, InternetAddress to, @Nullable InternetAddress replyTo, MultipartFile attachment ) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper( message, true );

        helper.setSubject( subject );
        helper.setText( content );
        helper.setTo( to );
        helper.setFrom( getAdminAddress() );
        if ( replyTo != null ) {
            helper.setReplyTo( replyTo );
        }

        if ( attachment.getOriginalFilename() != null ) {
            helper.addAttachment( attachment.getOriginalFilename(), attachment );
        } else {
            log.warn( String.format( "Attachment %s is lacking a filename, it will be discarded.", attachment ) );
        }

        return executorService.submit( () -> emailSender.send( message ) );
    }

    @Override
    public Future<?> sendSupportMessage( String message, String name, User user, String userAgent, @Nullable MultipartFile attachment, Locale locale ) throws MessagingException {
        InternetAddress replyTo = user.getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        String subject = messageSource.getMessage( "EmailService.sendSupportMessage.subject", new Object[]{ Messages.SHORTNAME }, locale );
        String content = "Name: " + name + "\r\n" +
                "Email: " + user.getEmail() + "\r\n" +
                "User-Agent: " + userAgent + "\r\n" +
                "Message: " + message + "\r\n";
        if ( attachment != null ) {
            content += "File Attached: " + attachment.getOriginalFilename() + "\r\n";
        }
        if ( attachment == null ) {
            return sendSimpleMessage( subject, content, getAdminAddress(), replyTo, null );
        } else {
            return sendMultipartMessage( subject, content, getAdminAddress(), replyTo, attachment );
        }
    }

    @Override
    public Future<?> sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException {
        URI url = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "updatePassword" )
                .queryParam( "id", "{id}" )
                .queryParam( "token", "{token}" )
                .build( new HashMap<String, String>() {
                    {
                        put( "id", user.getId().toString() );
                        put( "token", token.getToken() );
                    }
                } );

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofLocalizedDateTime( FormatStyle.SHORT )
                .withLocale( locale )
                .withZone( ZoneId.systemDefault() );

        // password reset always go through the primary email
        InternetAddress to = new InternetAddress( user.getEmail() );
        String subject = messageSource.getMessage( "EmailService.sendResetTokenMessage.subject", new Object[]{ Messages.SHORTNAME }, locale );
        String content = messageSource.getMessage( "EmailService.sendResetTokenMessage", new String[]{
                user.getProfile().getName(), url.toString(), dateTimeFormatter.format( token.getExpiryDate() ) }, locale );

        return sendSimpleMessage( subject, content, to, null, null );
    }

    @Override
    public Future<?> sendRegistrationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException {
        String registrationWelcome = messageSource.getMessage( "rdp.site.email.registration-welcome", new Object[]{ siteSettings.getHostUrl().toString(), Messages.SHORTNAME }, locale );
        String registrationEnding = messageSource.getMessage( "rdp.site.email.registration-ending", new String[]{ siteSettings.getContactEmail() }, locale );
        // registration always go through the primary email
        InternetAddress recipientAddress = new InternetAddress( user.getEmail() );
        String subject = messageSource.getMessage( "EmailService.sendRegistrationMessage.subject", new Object[]{ Messages.SHORTNAME }, locale );
        URI confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "registrationConfirm" )
                .queryParam( "token", "{token}" )
                .build( Collections.singletonMap( "token", token.getToken() ) );
        String message = registrationWelcome + "\r\n\r\n" +
                messageSource.getMessage( "EmailService.sendRegistrationMessage", new String[]{ confirmationUrl.toString() }, locale ) + "\r\n\r\n" +
                registrationEnding;
        return sendSimpleMessage( subject, message, recipientAddress, null, null );
    }

    @Override
    public Future<?> sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException {
        Assert.notNull( user.getProfile().getContactEmail(), "User must have a contact email." );
        InternetAddress recipientAddress = new InternetAddress( user.getProfile().getContactEmail() );
        String subject = messageSource.getMessage( "EmailService.sendContactEmailVerificationMessage.subject", new Object[]{ Messages.SHORTNAME }, locale );
        URI confirmationUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "user/verify-contact-email" )
                .queryParam( "token", "{token}" )
                .build( Collections.singletonMap( "token", token.getToken() ) );
        String message = messageSource.getMessage( "EmailService.sendContactEmailVerificationMessage", new String[]{ confirmationUrl.toString() }, locale );
        return sendSimpleMessage( subject, message, recipientAddress, null, null );
    }

    @Override
    public Future<?> sendUserRegisteredEmail( User user ) throws MessagingException {
        // unfortunately, there's no way to tell the dmin locale
        Locale locale = Locale.getDefault();
        String subject = messageSource.getMessage( "EmailService.sendUserRegisteredEmail.subject", new Object[]{ Messages.SHORTNAME }, locale );
        String content = messageSource.getMessage( "EmailService.sendUserRegisteredEmail", new String[]{ user.getEmail() }, locale );
        return sendSimpleMessage( subject, content, getAdminAddress(), null, null );
    }

    @Override
    public Future<?> sendUserGeneAccessRequest( UserGene userGene, User replyTo, String reason ) throws MessagingException {
        URI viewUserUrl = UriComponentsBuilder.fromUri( siteSettings.getHostUrl() )
                .path( "search/user/{userId}" )
                .build( Collections.singletonMap( "userId", replyTo.getId() ) );
        InternetAddress to = userGene.getUser().getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        InternetAddress replyToAddress = replyTo.getVerifiedContactEmail().orElseThrow( () -> new MessagingException( "Could not find a verified email address for user." ) );
        // unfortunately, there's no way to tell the recipient locale for now
        Locale locale = Locale.getDefault();
        String subject = messageSource.getMessage( "EmailService.sendUserGeneAccessRequest.subject", new Object[]{ Messages.SHORTNAME }, locale );
        String content = messageSource.getMessage( "EmailService.sendUserGeneAccessRequest",
                new String[]{ replyTo.getProfile().getFullName(), userGene.getSymbol(), reason, viewUserUrl.toString() }, locale );
        return sendSimpleMessage( subject, content, to, replyToAddress, getAdminAddress() );
    }

    private InternetAddress getAdminAddress() throws AddressException {
        try {
            return new InternetAddress( siteSettings.getAdminEmail(), messageSource.getMessage( Messages.SHORTNAME, Locale.getDefault() ) );
        } catch ( UnsupportedEncodingException e ) {
            log.error( String.format( "Could not encode the admin email personal, please set %s correctly.", Messages.SHORTNAME.getCode() ), e );
            return new InternetAddress( siteSettings.getAdminEmail() );
        }
    }
}
