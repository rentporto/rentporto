package pt.rentporto.web;

/**
 * Endpoint that is part of the contract but not implemented yet; answered with 501.
 */
public class EndpointNotImplementedException extends RuntimeException {

    public EndpointNotImplementedException() {
        super("This endpoint is part of the contract but is not implemented yet");
    }
}
