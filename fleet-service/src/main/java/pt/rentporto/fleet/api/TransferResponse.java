package pt.rentporto.fleet.api;

import java.util.UUID;

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
}
