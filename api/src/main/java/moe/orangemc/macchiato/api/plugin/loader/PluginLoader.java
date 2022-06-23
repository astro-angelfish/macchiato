package moe.orangemc.macchiato.plugin.loader;

import moe.orangemc.macchiato.api.plugin.ExposedPluginDescription;
import moe.orangemc.macchiato.api.plugin.Plugin;
import moe.orangemc.macchiato.plugin.InvalidDescriptionException;
import moe.orangemc.macchiato.plugin.InvalidPluginException;
import moe.orangemc.macchiato.plugin.PluginDescriptionFile;
import moe.orangemc.macchiato.plugin.UnknownDependencyException;

import java.io.File;
import java.util.regex.Pattern;

public interface PluginLoader {
    Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException;
    ExposedPluginDescription getPluginDescription(File file) throws InvalidDescriptionException;
    void enablePlugin(Plugin plugin);
    void disablePlugin(Plugin plugin);
    Pattern[] getPluginFileFilters();
}
