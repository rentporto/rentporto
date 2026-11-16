package pt.rentporto.web;

import java.util.List;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Answers every error as a ProblemDetail (RFC 9457) with a {@code code} property: the business reason for a
 * {@link BusinessException}, {@code VALIDATION_FAILED} with the invalid fields, and the status name otherwise.
 *
 * <p>It has the lowest precedence, so a service can handle specific exceptions in its own advice.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

    static final String CODE = "code";
    static final String ERRORS = "errors";
    static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException e, WebRequest request) {
        HttpStatus status = switch (e.category()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        return problem(e, status, e.getMessage(), e.code(), request);
    }

    @ExceptionHandler(EndpointNotImplementedException.class)
    ResponseEntity<Object> handleNotImplemented(EndpointNotImplementedException e, WebRequest request) {
        return problem(e, HttpStatus.NOT_IMPLEMENTED, e.getMessage(), HttpStatus.NOT_IMPLEMENTED.name(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception e, WebRequest request) {
        logger.error("Unexpected error", e);
        return problem(e, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", "INTERNAL_ERROR", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), message(error.getDefaultMessage())))
                .toList();
        return invalid(e, e.getBody(), errors, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new InvalidField(
                                result.getMethodParameter().getParameterName(), message(error.getDefaultMessage()))))
                .toList();
        return invalid(e, e.getBody(), errors, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(CODE))) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            problem.setProperty(CODE, status != null ? status.name() : String.valueOf(statusCode.value()));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> problem(
            Exception e, HttpStatus status, String detail, String code, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty(CODE, code);
        return handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    private ResponseEntity<Object> invalid(
            Exception e,
            ProblemDetail body,
            List<InvalidField> errors,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        body.setProperty(CODE, VALIDATION_FAILED);
        body.setProperty(ERRORS, errors);
        return handleExceptionInternal(e, body, headers, status, request);
    }

    private static String message(String defaultMessage) {
        return Objects.requireNonNullElse(defaultMessage, "is invalid");
    }

    /**
     * Field or parameter that failed validation.
     *
     * @param field   name of the field or parameter
     * @param message why it is invalid
     */
    record InvalidField(String field, String message) {
    }
}
