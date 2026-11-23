package pt.rentporto.gateway.docs;

import pt.rentporto.web.BusinessException;

/**
 * A service did not answer with its OpenAPI document, so its documentation cannot be shown now.
 */
class DocsUnavailableException extends BusinessException {

    DocsUnavailableException(String service) {
        super("API_DOCS_UNAVAILABLE", Category.UNAVAILABLE,
                "The documentation of " + service + " is not available; try again later");
    }
}
