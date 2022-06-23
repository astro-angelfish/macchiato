package moe.orangemc.macchiato.plugin.loader.jvm;

import com.google.common.base.Preconditions;
import moe.orangemc.macchiato.MacchiatoDebugger;
import moe.orangemc.macchiato.api.plugin.*;
import moe.orangemc.macchiato.api.plugin.loader.PluginLoader;
import moe.orangemc.macchiato.api.terminal.TerminalColor;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;
import moe.orangemc.macchiato.plugin.*;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class JavaPluginLoader implements PluginLoader {
    private final MacchiatoDebugger debugger;

    private final Pattern[] fileFilters = new Pattern[]{Pattern.compile("\\.jar$")};
    private final List<SimpleJvmPluginClassLoader> loaders = new CopyOnWriteArrayList<>();
    private final LibraryLoader libraryLoader;
    private final WrappedTerminal startupTerminal;

    public JavaPluginLoader(MacchiatoDebugger debugger, WrappedTerminal startupTerminal) {
        this.debugger = debugger;
        this.startupTerminal = startupTerminal;

        LibraryLoader libraryLoader = null;
        try {
            libraryLoader = new LibraryLoader(startupTerminal);
        } catch (NoClassDefFoundError e) {
            startupTerminal.println(TerminalColor.RED + "Macchiato could not initialize the library loader. Is something missing 0.0??");
        }
        this.libraryLoader = libraryLoader;
    }

    Class<?> getClassByName(final String name, boolean resolve, PluginDescriptionFile description) {
        for (SimpleJvmPluginClassLoader loader : loaders) {
            try {
                return loader.loadClass0(name, resolve, false, ((SugarPluginManager) MacchiatoDebugger.getInstance().getPluginManager()).isTransitiveDepend(description, (PluginDescriptionFile) loader.plugin.getDescription()));
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    public Plugin loadPlugin(final File file) throws InvalidPluginException {
        Preconditions.checkArgument(file != null, "File cannot be null");

        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(file.getPath() + " does not exist"));
        }

        final PluginDescriptionFile description;
        try {
            description = getPluginDescription(file);
        } catch (InvalidDescriptionException ex) {
            throw new InvalidPluginException(ex);
        }

        final File parentFile = file.getParentFile();
        final File dataFolder = new File(parentFile, description.getName());
        final File oldDataFolder = new File(parentFile, description.getRawName());

        // Found old data folder
        if (dataFolder.equals(oldDataFolder)) {
            // They are equal -- nothing needs to be done!
        } else if (dataFolder.isDirectory() && oldDataFolder.isDirectory()) {
            startupTerminal.println(TerminalColor.YELLOW + "Macchiato found that the old-data folder of " + description.getFullName() + "(file: " + file + ") which is " + oldDataFolder + " and the new data folder " + dataFolder + " exists. So Macchiato can not migrate them automatically.");
        } else if (oldDataFolder.isDirectory() && !dataFolder.exists()) {
            if (!oldDataFolder.renameTo(dataFolder)) {
                throw new InvalidPluginException("Macchiato failed to rename old data folder: `" + oldDataFolder + "' to: `" + dataFolder + "'");
            }

            startupTerminal.println("Migrated " + description.getName() + "'s old data folder: " + oldDataFolder + " into new one: " + dataFolder);
        }

        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidPluginException(String.format(
                    "Projected datafolder: `%s' for %s (%s) exists and is not a directory",
                    dataFolder,
                    description.getFullName(),
                    file
            ));
        }

        for (final String pluginName : description.getDependencies()) {
            Plugin current = MacchiatoDebugger.getInstance().getPluginManager().getPlugin(pluginName);

            if (current == null) {
                throw new UnknownDependencyException("Unknown dependency " + pluginName + ". Please download and install " + pluginName + " to run this plugin.");
            }
        }

        final SimpleJvmPluginClassLoader loader;
        try {
            loader = new SimpleJvmPluginClassLoader(this, getClass().getClassLoader(), description, dataFolder, file, (libraryLoader != null) ? libraryLoader.createLoader(description) : null, startupTerminal);
        } catch (InvalidPluginException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InvalidPluginException(ex);
        }

        loaders.add(loader);

        return loader.plugin;
    }

    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        Preconditions.checkArgument(file != null, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("plugin.yml");

            if (entry == null) {
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain plugin.yml"));
            }

            stream = jar.getInputStream(entry);

            return new PluginDescriptionFile(stream);

        } catch (IOException | YAMLException e) {
            throw new InvalidDescriptionException(e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void enablePlugin(Plugin plugin) {

    }

    @Override
    public void disablePlugin(Plugin plugin) {

    }

    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }
}
