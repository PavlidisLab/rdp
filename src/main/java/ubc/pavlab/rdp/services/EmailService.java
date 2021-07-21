package ubc.pavlab.rdp.services;

import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 *
 */
public interface EmailService {

    void sendSupportMessage( String message, String name, User user, String userAgent, MultipartFile attachment, Locale locale ) throws MessagingException;

    void sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException;

    void sendRegistrationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException;

    void sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException;

    void sendUserRegisteredEmail( User user ) throws MessagingException;

    void sendUserGeneAccessRequest( UserGene userGene, User by, String reason ) throws MessagingException;
}
