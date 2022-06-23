package moe.orangemc.macchiato.plugin;

public class InvalidPluginException extends Exception {
    public InvalidPluginException(final Throwable cause) {
        super(cause);
    }

    public InvalidPluginException() {

    }

    public InvalidPluginException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidPluginException(final String message) {
        super(message);
    }
}
