package pt.rentporto.notification.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.notification.application.CustomerEmails;

@RestController
class NotificationsController implements NotificationsApi {

    private final CustomerEmails emails;

    NotificationsController(CustomerEmails emails) {
        this.emails = emails;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<NotificationResponse> notifications(UUID customerId) {
        return emails.history(customerId).stream().map(NotificationResponse::from).toList();
    }
}
