package pt.rentporto.gateway.routing;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/**
 * Answers 503 when the gateway cannot reach a service, instead of the 500 of an unexpected error.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class UnavailableServiceAdvice {

    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail serviceUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "The service is not available; try again later");
        problem.setProperty("code", HttpStatus.SERVICE_UNAVAILABLE.name());
        return problem;
    }
}
