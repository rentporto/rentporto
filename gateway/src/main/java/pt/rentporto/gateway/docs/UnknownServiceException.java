package pt.rentporto.gateway.docs;

import pt.rentporto.web.error.BusinessException;

/**
 * There is no service with an API by the name asked for.
 */
class UnknownServiceException extends BusinessException {

    UnknownServiceException(String service) {
        super("SERVICE_NOT_FOUND", Category.NOT_FOUND, "There is no service " + service + " with an API");
    }
}
