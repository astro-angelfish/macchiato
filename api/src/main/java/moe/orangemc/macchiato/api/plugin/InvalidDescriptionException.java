package moe.orangemc.macchiato.plugin;

public class InvalidDescriptionException extends Exception {
    public InvalidDescriptionException(final Throwable cause, final String message) {
        super(message, cause);
    }

    public InvalidDescriptionException(final Throwable cause) {
        super("Invalid plugin.yml", cause);
    }

    public InvalidDescriptionException(final String message) {
        super(message);
    }

    public InvalidDescriptionException() {
        super("Invalid plugin.yml");
    }
}
