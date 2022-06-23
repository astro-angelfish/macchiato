package moe.orangemc.macchiato.cli;

public class CliFailureException extends Exception {
    CliFailureException(String msg, Throwable t) {
        super(msg, t);
    }
}
