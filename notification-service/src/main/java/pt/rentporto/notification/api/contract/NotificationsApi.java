package pt.rentporto.notification.api.contract;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.rentporto.notification.api.response.NotificationResponse;

/**
 * History of the notifications sent.
 */
@Tag(name = "Notifications")
public interface NotificationsApi {

    /**
     * Lists the notifications of a customer, the ones that failed included. Roles: ADMIN.
     *
     * @param customerId customer whose notifications are listed
     * @return the notifications, most recent attempt first
     */
    @GetMapping("/notifications")
    List<NotificationResponse> notifications(@RequestParam @NotNull UUID customerId);
}
