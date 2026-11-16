package pt.rentporto.notification.api;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class NotificationsController implements NotificationsApi {

    @Override
    public List<NotificationResponse> notifications(UUID customerId) {
        throw new EndpointNotImplementedException();
    }
}
