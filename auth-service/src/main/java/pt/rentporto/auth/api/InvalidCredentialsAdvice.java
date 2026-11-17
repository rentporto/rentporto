package pt.rentporto.auth.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.rentporto.auth.application.InvalidCredentialsException;

/**
 * Answers a failed login with 401 and the code {@code INVALID_CREDENTIALS}, the same whether the email is unknown or
 * the password is wrong.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class InvalidCredentialsAdvice {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problem.setProperty("code", "INVALID_CREDENTIALS");
        return problem;
    }
}
