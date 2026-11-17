package pt.rentporto.gateway.ratelimit;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The client went over its limit: 429 with {@code Retry-After}, answered as a ProblemDetail by the common-web advice.
 */
class TooManyRequestsException extends ErrorResponseException {

    /**
     * Creates the exception.
     *
     * @param retryAfter how long until the client can make another request
     */
    TooManyRequestsException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, problem(seconds(retryAfter)), null);
        getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds(retryAfter)));
    }

    private static ProblemDetail problem(long seconds) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Too many requests; try again in " + seconds + " seconds");
    }

    private static long seconds(Duration retryAfter) {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}
