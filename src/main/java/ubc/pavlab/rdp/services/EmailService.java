package ubc.pavlab.rdp.services;

import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 *
 */
public interface EmailService {

    void sendSupportMessage( String message, String name, User user, HttpServletRequest request, MultipartFile attachment ) throws MessagingException;

    void sendResetTokenMessage( User user, PasswordResetToken token, Locale locale ) throws MessagingException;

    void sendRegistrationMessage( User user, VerificationToken token ) throws MessagingException;

    void sendContactEmailVerificationMessage( User user, VerificationToken token ) throws MessagingException;

    void sendUserRegisteredEmail( User user ) throws MessagingException;
}
