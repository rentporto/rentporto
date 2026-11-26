package pt.rentporto.notification.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import pt.rentporto.notification.application.exception.EmailNotSentException;
import pt.rentporto.notification.application.port.Mailer;

/**
 * Sends the emails by SMTP, to Mailpit in development and in k3d. The message keeps the {@code Message-ID} it is
 * given, which the mail library would otherwise replace with a new one.
 */
@Component
class SmtpMailer implements Mailer {

    private final JavaMailSenderImpl sender;
    private final String from;

    SmtpMailer(
            JavaMailSenderImpl sender,
            @Value("${rentporto.notification.from:RentPorto <noreply@rentporto.pt>}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(Email email) {
        try {
            MimeMessage message = new MimeMessage(sender.getSession()) {
                @Override
                protected void updateMessageID() throws MessagingException {
                    setHeader("Message-ID", email.messageId());
                }
            };
            message.setFrom(new InternetAddress(from));
            message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(email.to()));
            message.setSubject(email.subject(), StandardCharsets.UTF_8.name());
            message.setText(email.body(), StandardCharsets.UTF_8.name());
            sender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailNotSentException("The email to " + email.to() + " was not sent: " + e.getMessage(), e);
        }
    }
}
