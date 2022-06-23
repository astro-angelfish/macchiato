package macchiato.jar;

import macchiato.CoffeeConfiguration;
import macchiato.cli.CliColor;
import macchiato.cli.CliManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassCache {
    private final CoffeeConfiguration coffeeConfiguration;
    private final Map<String, List<ClassNode>> classCache = new HashMap<>();
    private final Set<String> loadedFiles = new HashSet<>();
    private final CliManager cliManager;

    public JarClassCache(CoffeeConfiguration configuration, CliManager cliManager) {
        coffeeConfiguration = configuration;
        this.cliManager = cliManager;
    }
    public List<ClassNode> findClass(String name) {
        if (classCache.containsKey(name)) {
            return classCache.get(name);
        }

        File[] jarFiles = coffeeConfiguration.getJarResources();
        for (File jar : jarFiles) {
            try (JarFile parsedJar = new JarFile(jar)) {
                Enumeration<JarEntry> entries = parsedJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry je = entries.nextElement();

                    if (!je.getName().endsWith(".class") || loadedFiles.contains(jar + "#" + je.getName())) {
                        continue;
                    }

                    loadedFiles.add(jar + "#" + je.getName());

                    try (InputStream is = parsedJar.getInputStream(je)) {
                        ClassReader classReader = new ClassReader(is);
                        ClassNode classNode = new ClassNode(Opcodes.ASM9);
                        classReader.accept(classNode, 0);

                        putClassNode(classNode.name, classNode);
                    } catch (IOException e) {
                        cliManager.println(CliColor.YELLOW + "Warning: Failed to load class file: " + je.getName());
                        cliManager.printException(e, CliColor.YELLOW);
                    }
                }
            } catch (IOException e) {
                cliManager.println(CliColor.YELLOW + "Failed to open jar file: " + jar.getAbsoluteFile());
                cliManager.printException(e, CliColor.YELLOW);
            }
        }

        return classCache.get(name);
    }

    private void putClassNode(String name, ClassNode classNode) {
        List<ClassNode> classNodes = classCache.get(name);
        if (classNodes == null) {
            classNodes = new ArrayList<>();
        }

        classNodes.add(classNode);
        classCache.put(name, classNodes);
    }
}
