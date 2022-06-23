package moe.orangemc.macchiato.plugin.loader.jvm;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import moe.orangemc.macchiato.MacchiatoDebugger;
import moe.orangemc.macchiato.api.plugin.ExposedPluginDescription;
import moe.orangemc.macchiato.api.plugin.jvm.JvmPlugin;
import moe.orangemc.macchiato.api.terminal.TerminalColor;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;
import moe.orangemc.macchiato.api.plugin.InvalidPluginException;
import moe.orangemc.macchiato.plugin.PluginDescriptionFile;
import moe.orangemc.macchiato.plugin.SugarPluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class PluginClassLoader extends URLClassLoader {
    private final JavaPluginLoader loader;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final PluginDescriptionFile description;
    private final File dataFolder;
    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;
    private final ClassLoader libraryLoader;
    final JvmPlugin plugin;
    private JvmPlugin pluginInit;
    private IllegalStateException pluginState;
    private final Set<String> seenIllegalAccess = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final WrappedTerminal startupCli;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    PluginClassLoader(final JavaPluginLoader loader, final ClassLoader parent, final PluginDescriptionFile description, final File dataFolder, final File file, ClassLoader libraryLoader, WrappedTerminal startupCli) throws IOException, InvalidPluginException {
        super(new URL[] {file.toURI().toURL()}, parent);
        Preconditions.checkArgument(loader != null, "Loader cannot be null");

        this.loader = loader;
        this.description = description;
        this.dataFolder = dataFolder;
        this.jar = new JarFile(file);
        this.manifest = jar.getManifest();
        this.url = file.toURI().toURL();
        this.libraryLoader = libraryLoader;
        this.startupCli = startupCli;

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMainClass(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidPluginException("Cannot find main class `" + description.getMainClass() + "'", ex);
            }

            Class<? extends JvmPlugin> pluginClass;
            try {
                pluginClass = jarClass.asSubclass(JvmPlugin.class);
            } catch (ClassCastException ex) {
                throw new InvalidPluginException("main class `" + description.getMainClass() + "' does not extend JavaPlugin", ex);
            }

            plugin = pluginClass.getConstructor(String.class, ExposedPluginDescription.class).newInstance(description.getName(), description);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new InvalidPluginException("No public constructor", e);
        } catch (InstantiationException e) {
            throw new InvalidPluginException("Abnormal plugin type", e);
        } catch (InvocationTargetException e) {
            throw new InvalidPluginException("Exception while initializing the constructor", e);
        }
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true, true);
    }

    Class<?> loadClass0(String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        try {
            Class<?> result = super.loadClass(name, resolve);

            if (checkGlobal || result.getClassLoader() == this) {
                return result;
            }
        } catch (ClassNotFoundException ignored) {
        }

        if (checkLibraries && libraryLoader != null) {
            try {
                return libraryLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (checkGlobal) {
            // This ignores the libraries of other plugins, unless they are transitive dependencies.
            Class<?> result = loader.getClassByName(name, resolve, description);

            if (result != null) {
                // If the class was loaded from a library instead of a PluginClassLoader, we can assume that its associated plugin is a transitive dependency and can therefore skip this check.
                if (result.getClassLoader() instanceof PluginClassLoader) {
                    PluginDescriptionFile provider = ((PluginClassLoader) result.getClassLoader()).description;

                    if (provider != description
                            && !seenIllegalAccess.contains(provider.getName())
                            && !((SugarPluginManager) MacchiatoDebugger.getInstance().getPluginManager()).isTransitiveDepend(description, provider)) {

                        seenIllegalAccess.add(provider.getName());
                        if (plugin != null) {
                            startupCli.println(TerminalColor.YELLOW + "Macchiato has found a plugin called " + provider.getFullName() + " that uses the class called " + name + " which from another plugin. But it should add the plugin where the class in into their depend/softdepend list!");
                        } else {
                            startupCli.println(TerminalColor.YELLOW + "Macchiato has found a plugin called " + provider.getFullName() + " that uses the class called " + name + " which from " + description.getName() + ". But it should add the plugin where the class in into their depend/softdepend list!");
                        }
                    }
                }

                return result;
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            JarEntry entry = jar.getJarEntry(path);

            if (entry != null) {
                byte[] classBytes;

                try (InputStream is = jar.getInputStream(entry)) {
                    classBytes = ByteStreams.toByteArray(is);
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }

                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String pkgName = name.substring(0, dot);
                    if (getDefinedPackage(pkgName) == null) {
                        try {
                            if (manifest != null) {
                                definePackage(pkgName, manifest, url);
                            } else {
                                definePackage(pkgName, null, null, null, null, null, null, null);
                            }
                        } catch (IllegalArgumentException ex) {
                            if (getDefinedPackage(pkgName) == null) {
                                throw new IllegalStateException("Cannot find package " + pkgName);
                            }
                        }
                    }
                }

                CodeSigner[] signers = entry.getCodeSigners();
                CodeSource source = new CodeSource(url, signers);

                result = defineClass(name, classBytes, 0, classBytes.length, source);
            }

            if (result == null) {
                result = super.findClass(name);
            }
            classes.put(name, result);
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    Collection<Class<?>> getClasses() {
        return classes.values();
    }

    synchronized void initialize(JvmPlugin jvmPlugin) {
        Preconditions.checkArgument(jvmPlugin != null, "Initializing plugin cannot be null");
        Preconditions.checkArgument(jvmPlugin.getClass().getClassLoader() == this, "Cannot initialize plugin outside of this class loader");
        if (this.plugin != null || this.pluginInit != null) {
            throw new IllegalArgumentException("Plugin already initialized!", pluginState);
        }

        pluginState = new IllegalStateException("Initial initialization");
        this.pluginInit = jvmPlugin;

        jvmPlugin.init(loader, MacchiatoDebugger.getInstance().getCommandMap(), dataFolder, MacchiatoDebugger.getInstance());
        jvmPlugin.setTerminalManager(startupCli);
    }
}
