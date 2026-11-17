package pt.rentporto.notification.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class NotificationsController implements NotificationsApi {

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<NotificationResponse> notifications(UUID customerId) {
        throw new EndpointNotImplementedException();
    }
}
