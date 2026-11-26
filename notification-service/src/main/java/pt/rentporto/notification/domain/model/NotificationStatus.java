package pt.rentporto.notification.domain.model;

/**
 * Outcome of the last attempt to send a notification.
 */
public enum NotificationStatus {

    /** The email was accepted by the mail server. */
    SENT,
    /** The email could not be sent; the event is tried again. */
    FAILED
}
