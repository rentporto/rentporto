package pt.rentporto.fleet.api.response;

import java.util.UUID;
import pt.rentporto.fleet.api.model.TransferStatus;
import pt.rentporto.fleet.domain.model.Transfer;

/**
 * Transfer of a vehicle between stations.
 *
 * @param transferId   transfer identifier
 * @param licensePlate licence plate of the vehicle
 * @param fromStation  code of the origin station
 * @param toStation    code of the destination station
 * @param status       current state
 */
public record TransferResponse(
        UUID transferId, String licensePlate, String fromStation, String toStation, TransferStatus status) {

    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.id(),
                transfer.plate().value(),
                transfer.fromStation(),
                transfer.toStation(),
                TransferStatus.valueOf(transfer.status().name()));
    }
}
