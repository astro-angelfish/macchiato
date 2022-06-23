package moe.orangemc.macchiato.cli.command;

import moe.orangemc.macchiato.api.command.CommandBase;
import moe.orangemc.macchiato.api.command.CommandManager;
import moe.orangemc.macchiato.api.terminal.TerminalColor;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;
import moe.orangemc.macchiato.cli.DefaultCli;
import moe.orangemc.macchiato.api.command.util.CommandException;
import moe.orangemc.macchiato.api.command.util.SyntaxException;

import java.util.HashMap;
import java.util.Map;

public class CommandMap implements CommandManager {
    private final Map<String, CommandBase> commands = new HashMap<>();

    @Override
    public void registerCommand(CommandBase command) {
        commands.put(command.getName(), command);

        String[] aliases = command.getAliases();
        for (String alias : aliases) {
            commands.put(alias, command);
        }
    }

    public void executeCommand(WrappedTerminal terminal, String commandLabel, String[] args) {
        if (!commands.containsKey(commandLabel)) {
            terminal.println(TerminalColor.RED + "Unknown command. Type \"help\" for help.");
            return;
        }

        CommandBase command = commands.get(commandLabel);

        try {
            command.execute(terminal, args);
        } catch (CommandException e) {
            if (e instanceof SyntaxException syntaxException) {
                terminal.println(TerminalColor.RED + "Usage: /" + commandLabel + " " + syntaxException.getMessage());
            } else {
                terminal.println(TerminalColor.RED + "Your command has logical errors and Macchiato refuses to execute it.");
                terminal.println(TerminalColor.RED + "Macchiato think it is bad because: " + e.getMessage());
            }
        } catch (Exception e) {
            terminal.println(TerminalColor.YELLOW + "Macchiato has encountered some issues while performing command: ");
            terminal.printException(e, TerminalColor.YELLOW);
        } catch (Error e) {
            terminal.println(TerminalColor.RED + "Macchiato has encountered some serious issues while performing command: ");
            terminal.printException(e);
            System.exit(1);
        }
    }
}
