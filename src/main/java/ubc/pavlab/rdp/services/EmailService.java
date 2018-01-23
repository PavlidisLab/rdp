package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by mjacobson on 19/01/18.
 */
@Service
public class EmailService {

    @Autowired
    private SiteSettings siteSettings;

    @Autowired
    private JavaMailSender emailSender;

    public void sendSimpleMessage( String subject, String content, String to ) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setSubject( subject );
        email.setText( content );
        email.setTo( to );
        email.setFrom( siteSettings.getAdminEmail() );

        emailSender.send( email );

    }

    public void sendMessage( String subject, String content, String to, CommonsMultipartFile attachment ) throws MessagingException {

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper( message, true );

        helper.setSubject( subject );
        helper.setText( content );
        helper.setTo( to );
        helper.setFrom( siteSettings.getAdminEmail() );

        if ( attachment != null ) {
            helper.addAttachment( attachment.getOriginalFilename(), attachment );
        }

        emailSender.send( message );

    }

    public void sendSupportMessage( String message, String name, User user, HttpServletRequest request,
                                    CommonsMultipartFile attachment ) throws MessagingException {
        String content =
                "Name: " + name + "\r\n" +
                        "Email: " + user.getEmail() + "\r\n" +
                        "User-Agent: " + request.getHeader( "User-Agent" ) + "\r\n" +
                        "Message: " + message + "\r\n" +
                        "File Attached: " + String.valueOf( attachment != null && !attachment.getOriginalFilename().equals( "" ) );

        sendMessage( "Registry Help - Contact Support", content, siteSettings.getAdminEmail(), attachment );
    }

    public void sendResetTokenMessage( String token, User user ) throws MessagingException {
        String url = siteSettings.getFullUrl() + "updatePassword?id=" + user.getId() + "&token=" + token;

        String content =
                "Hello " + user.getProfile().getName() + ",\r\n\r\n" +
                        "We recently received a request that you want to reset your password. " +
                        "In order to reset your password, please click the confirmation link below:\r\n\r\n" +
                        url + "\r\n\r\n" +
                        "If you did not initiate this request, please disregard and delete this e-mail. " +
                        "Please note that this link will expire in " + PasswordResetToken.EXPIRATION + " hours.";


        sendMessage( "Reset Password", content, user.getEmail(), null );
    }

    public void sendRegistrationMessage( User user, String token ) {
        String recipientAddress = user.getEmail();
        String subject = "Registration Confirmation";
        String confirmationUrl = siteSettings.getFullUrl() + "registrationConfirm?token=" + token;
        String message =
                "Thank you for registering for the " + siteSettings.getFullname() + " as a model organism researcher. (" + siteSettings.getFullUrl() + ").\r\n\r\n" +
                        "Please confirm your registration by clicking on the following link:\r\n\r\n" +
                        confirmationUrl + "\r\n\r\n" +
                        "You will then be able to log in using the password you provided, and start filling in your profile.\r\n\r\n" +
                        "If you have questions or difficulties with registration please feel free to contact us: " + siteSettings.getContactEmail();
        sendSimpleMessage( subject, message, recipientAddress );
    }

}
