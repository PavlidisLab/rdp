package ubc.pavlab.rdp.services;

import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.concurrent.Future;

/**
 *
 */
public interface EmailService {

    Future<Void> sendSupportMessage( String message, String name, User user, String userAgent, MultipartFile attachment, Locale locale ) throws MessagingException;

    Future<Void> sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException;

    Future<Void> sendRegistrationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException;

    Future<Void> sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException;

    Future<Void> sendUserRegisteredEmail( User user ) throws MessagingException;

    Future<Void> sendUserGeneAccessRequest( UserGene userGene, User by, String reason ) throws MessagingException;
}
