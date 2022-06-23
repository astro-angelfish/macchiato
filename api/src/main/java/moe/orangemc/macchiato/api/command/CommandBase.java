package moe.orangemc.macchiato.cli.command;

import moe.orangemc.macchiato.cli.CliManager;
import moe.orangemc.macchiato.cli.command.util.CommandException;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandBase {
    public abstract String getName();

    public String[] getAliases() {
        return new String[0];
    }

    public List<String> tabComplete(String[] args) {
        return new ArrayList<>();
    }

    public abstract void execute(CliManager cliManager, String[] args) throws CommandException;
}
