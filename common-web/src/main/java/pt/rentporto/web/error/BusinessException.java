package pt.rentporto.web.error;

/**
 * Broken business rule, answered as a ProblemDetail with its {@code code}. Plain Java, so domain classes can extend
 * it without depending on HTTP; the category decides the status code.
 */
public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final Category category;

    /**
     * Creates the exception.
     *
     * @param code     business reason, e.g. {@code QUOTE_EXPIRED}
     * @param category kind of rule that was broken
     * @param message  detail shown to the client
     */
    protected BusinessException(String code, Category category, String message) {
        super(message);
        this.code = code;
        this.category = category;
    }

    /**
     * Business reason of the error.
     *
     * @return the code, e.g. {@code QUOTE_EXPIRED}
     */
    public String code() {
        return code;
    }

    /**
     * Kind of rule that was broken.
     *
     * @return the category
     */
    public Category category() {
        return category;
    }

    /**
     * Kind of business rule that was broken.
     */
    public enum Category {
        /** The resource does not exist (404). */
        NOT_FOUND,
        /** The request conflicts with the current state, e.g. a quote already used (409). */
        CONFLICT,
        /** The request is well formed but breaks a rule, e.g. a driver not eligible (422). */
        UNPROCESSABLE,
        /** Another service the request needs did not answer; the client can try again (503). */
        UNAVAILABLE
    }
}
