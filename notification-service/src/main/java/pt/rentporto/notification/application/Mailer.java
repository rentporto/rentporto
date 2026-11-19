package pt.rentporto.notification.application;

/**
 * Sends emails. Implemented in the infrastructure.
 */
public interface Mailer {

    /**
     * Sends an email.
     *
     * @param email the email
     * @throws EmailNotSentException if the mail server did not accept it
     */
    void send(Email email);

    /**
     * Email to send.
     *
     * @param to        address of the recipient
     * @param subject   subject
     * @param body      plain text
     * @param messageId value of the {@code Message-ID} header, the same whenever the same event is sent
     */
    record Email(String to, String subject, String body, String messageId) {
    }
}
