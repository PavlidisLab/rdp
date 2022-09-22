package ubc.pavlab.rdp.services;

import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.VerificationToken;

import javax.mail.MessagingException;
import java.util.Locale;
import java.util.concurrent.Future;

/**
 *
 */
public interface EmailService {

    Future<?> sendSupportMessage( String message, String name, User user, String userAgent, MultipartFile attachment, Locale locale ) throws MessagingException;

    Future<?> sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException;

    Future<?> sendRegistrationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException;

    Future<?> sendContactEmailVerificationMessage( User user, VerificationToken token, Locale locale ) throws MessagingException;

    Future<?> sendUserRegisteredEmail( User user ) throws MessagingException;

    Future<?> sendUserGeneAccessRequest( UserGene userGene, User by, String reason ) throws MessagingException;
}
