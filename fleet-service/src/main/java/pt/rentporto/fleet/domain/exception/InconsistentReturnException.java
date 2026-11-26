package pt.rentporto.fleet.domain.exception;

/**
 * A returned vehicle does not match what the fleet knows: the rental has no vehicle or another one, the station does
 * not exist or the odometer went back. The return is not applied, and the event goes to the dead letter queue.
 */
public class InconsistentReturnException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what does not match
     */
    public InconsistentReturnException(String message) {
        super(message);
    }
}
