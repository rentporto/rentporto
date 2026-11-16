package pt.rentporto.booking.payment.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Receipts.
 */
@Tag(name = "Receipts")
public interface ReceiptsApi {

    /**
     * Reads a receipt. Roles: CUSTOMER (owner).
     *
     * @param id receipt identifier
     * @return the receipt
     */
    @GetMapping("/receipts/{id}")
    ReceiptResponse receipt(@PathVariable UUID id);
}
