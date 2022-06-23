package macchiato.jdi;

public class BridgeFailureException extends Exception {
    public BridgeFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public BridgeFailureException(String message) {
        super(message);
    }
}
