package api.service;

/**
 * A custom exception type for JBMC-related errors.
 */
public class JBMCException extends RuntimeException {

    public JBMCException(String message) {
        super(message);
    }

    public JBMCException(String message, Throwable cause) {
        super(message, cause);
    }
}