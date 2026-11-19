package pt.rentporto.notification.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.notification.domain.Recipient;
import pt.rentporto.notification.domain.Recipients;

/**
 * Recipients in memory.
 */
final class InMemoryRecipients implements Recipients {

    private final Map<UUID, Recipient> byId = new HashMap<>();

    @Override
    public Optional<Recipient> withId(UUID customerId) {
        return Optional.ofNullable(byId.get(customerId));
    }

    @Override
    public void save(Recipient recipient) {
        byId.put(recipient.id(), recipient);
    }
}
