package pt.rentporto.notification.application;

import pt.rentporto.notification.domain.PickupReminder;

/**
 * Text of the pick-up reminder, kept with the texts of the events. Implemented in the infrastructure.
 */
public interface ReminderTemplate {

    /**
     * Writes the reminder of a pick-up.
     *
     * @param reminder the reminder, pending
     * @return the notice, identified by {@link PickupReminder#noticeId()}
     */
    Notice pickupReminder(PickupReminder reminder);
}
