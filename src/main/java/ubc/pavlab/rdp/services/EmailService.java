package ubc.pavlab.rdp.services;

import org.springframework.web.multipart.MultipartFile;
import ubc.pavlab.rdp.model.User;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public interface EmailService {

    void sendSupportMessage( String message, String name, User user, HttpServletRequest request,
                             MultipartFile attachment ) throws MessagingException;

    void sendResetTokenMessage( String token, User user ) throws MessagingException;

    void sendRegistrationMessage( User user, String token );

    void sendUserRegisteredEmail( User user );
}
