package pt.rentporto.notification.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * History of the notifications sent.
 */
@Tag(name = "Notifications")
public interface NotificationsApi {

    /**
     * Lists the notifications sent to a customer. Roles: ADMIN.
     *
     * @param customerId customer whose notifications are listed
     * @return the notifications, most recent first
     */
    @GetMapping("/notifications")
    List<NotificationResponse> notifications(@RequestParam @NotNull UUID customerId);
}
