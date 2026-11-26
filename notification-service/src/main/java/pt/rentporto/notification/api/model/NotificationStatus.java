package pt.rentporto.notification.api.model;

/**
 * Outcome of sending a notification.
 */
public enum NotificationStatus {
    /** The email was accepted by the mail server. */
    SENT,
    /** The email could not be sent. */
    FAILED
}
