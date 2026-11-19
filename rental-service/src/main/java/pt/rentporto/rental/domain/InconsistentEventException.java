package pt.rentporto.rental.domain;

/**
 * An event does not match the rental agreements: there is no agreement for its reservation, or the agreement is past
 * the state the event changes. The event is not applied and, after its attempts, goes to the dead letter queue.
 */
public class InconsistentEventException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what does not match
     */
    public InconsistentEventException(String message) {
        super(message);
    }
}
