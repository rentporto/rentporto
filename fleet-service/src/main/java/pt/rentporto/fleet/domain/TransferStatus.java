package pt.rentporto.fleet.domain;

/**
 * State of a transfer between stations.
 */
public enum TransferStatus {

    /** The vehicle is on its way to the destination station. */
    IN_TRANSFER,
    /** The vehicle arrived at the destination station. */
    COMPLETED
}
