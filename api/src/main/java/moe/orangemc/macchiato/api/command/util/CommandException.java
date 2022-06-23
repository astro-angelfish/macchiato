package moe.orangemc.macchiato.cli.command.util;

public class CommandException extends Exception {
    public CommandException(String msg) {
        super(msg);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
