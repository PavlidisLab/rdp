package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by mjacobson on 19/01/18.
 */
@Service
public class EmailService {

    @Autowired
    private Environment env;

    @Autowired
    private JavaMailSender emailSender;

//    @Autowired
//    private Environment env;

    @Value( "${rdp.admin.email}" )
    private String adminEmail;

    public void sendSimpleMessage(String subject, String content, String to, CommonsMultipartFile attachment) throws MessagingException {

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setSubject(subject);
        helper.setText(content);
        helper.setTo(to);
        helper.setFrom(adminEmail);

        if ( attachment != null) {
            helper.addAttachment( attachment.getOriginalFilename(), attachment );
        }

        emailSender.send(message);

    }

    public void sendSupportMessage( String message, String name, User user, HttpServletRequest request,
                                    CommonsMultipartFile attachment) throws MessagingException {
        String content =
                "Name: " + name + "\r\n" +
                "Email: " + user.getEmail() + "\r\n" +
                "User-Agent: " + request.getHeader( "User-Agent" ) + "\r\n" +
                "Message: " + message + "\r\n" +
                "File Attached: " + String.valueOf(attachment != null && !attachment.getOriginalFilename().equals( "" ));

        sendSimpleMessage( "Registry Help - Contact Support", content, adminEmail, attachment );
    }

    public void sendResetTokenMessage( String token, User user) throws MessagingException {
        String url = env.getProperty( "rdp.baseurl" ) + "updatePassword?id=" + user.getId() + "&token=" + token;

        String content =
                "Hello " +  user.getProfile().getName() + ",\r\n\r\n" +
                        "We recently received a request that you want to reset your password. " +
                        "In order to reset your password, please click the confirmation link below:\r\n\r\n" +
                        url + "\r\n\r\n" +
                        "If you did not initiate this request, please disregard and delete this e-mail. " +
                        "Please note that this link will expire in " + PasswordResetToken.EXPIRATION +" hours.";



        sendSimpleMessage("Reset Password", content, user.getEmail(), null);
    }

}
