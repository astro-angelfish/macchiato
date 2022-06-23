package moe.orangemc.macchiato.plugin;

public class UnknownDependencyException extends RuntimeException {
    public UnknownDependencyException(final Throwable throwable) {
        super(throwable);
    }
    public UnknownDependencyException(final String message) {
        super(message);
    }
    public UnknownDependencyException(final Throwable throwable, final String message) {
        super(message, throwable);
    }
    public UnknownDependencyException() {

    }
}
