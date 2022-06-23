package moe.orangemc.macchiato.api.plugin;

import moe.orangemc.macchiato.api.Debugger;
import moe.orangemc.macchiato.api.command.CommandManager;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;

import java.io.File;

public abstract class JvmPlugin {
    private final String name;
    private PluginManager pluginManager = null;
    private CommandManager commandManager = null;
    private File dataFolder = null;
    private WrappedTerminal wrappedTerminal;
    private Debugger debugger = null;
    private final ExposedPluginDescription description;

    public JvmPlugin(String name, ExposedPluginDescription description) {
        this.name = name;
        this.description = description;
    }

    public final void init(PluginManager pm, CommandManager commandManager, File dataFolder, Debugger debugger) {
        this.pluginManager = pm;
        this.commandManager = commandManager;
        this.dataFolder = dataFolder;
        this.debugger = debugger;
    }

    public final CommandManager getCommandManager() {
        return commandManager;
    }

    public final void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    public final void setTerminalManager(WrappedTerminal wrappedTerminal) {
        this.wrappedTerminal = wrappedTerminal;
    }

    public final Debugger getDebugger() {
        return debugger;
    }

    public final WrappedTerminal getTerminalManager() {
        return wrappedTerminal;
    }

    public final File getDataFolder() {
        return dataFolder;
    }

    public final String getName() {
        return name;
    }

    public final ExposedPluginDescription getDescription() {
        return description;
    }

    public void onLoad() {

    }

    public void onEnable() {

    }

    public void onDisable() {

    }
}
