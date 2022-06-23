package macchiato;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MacchiatoMain {
    private static CoffeeConfiguration config = new CoffeeConfiguration();

    public static void main(String[] args) {
        createRuntimeConfig(args);

        new MacchiatoDebugger(config);
    }

    private static void createRuntimeConfig(String[] args) {
        OptionParser optionParser = new OptionParser();

        optionParser.acceptsAll(Arrays.asList("?", "h", "help"), "Print the manual");
        OptionSpec<Integer> portSpec = optionParser.acceptsAll(Arrays.asList("p", "port"), "Defines the port of the jwdp.").withRequiredArg().ofType(Integer.class);
        optionParser.acceptsAll(Arrays.asList("l", "listen"), "Defines if listening to the port is required");
        OptionSpec<File> sourceSpec = optionParser.acceptsAll(Arrays.asList("s", "source"), "Defines where the source is located").withRequiredArg().ofType(File.class);
        optionParser.acceptsAll(Arrays.asList("v", "verbose"), "Print additional information");
        OptionSpec<File> scriptSpec = optionParser.acceptsAll(Arrays.asList("S", "script"), "Defines the initial script file.").withRequiredArg().ofType(File.class);
        OptionSpec<String> hostSpec = optionParser.acceptsAll(Arrays.asList("h", "host"), "Host to bind to or connect to.").withRequiredArg().ofType(String.class);
        OptionSpec<File> jarSpec = optionParser.acceptsAll(Arrays.asList("j", "jar"), "Defines the location of jars.").withRequiredArg().ofType(File.class);
        optionParser.acceptsAll(Arrays.asList("f", "fx", "javafx"), "Defines whether the program to be debugged uses javafx");

        try {
            OptionSet optionSet = optionParser.parse(args);

            if (optionSet.has("?")) {
                try {
                    optionParser.printHelpOn(System.out);
                } catch (IOException ex) {
                    System.err.println("We can not even print a manual...");
                    ex.printStackTrace();
                }
                System.exit(0);
            }

            config.setListen(optionSet.has("l"));
            config.setVerbose(optionSet.has("v"));
            if (optionSet.has("s")) {
                config.setSourceDirectories(optionSet.valuesOf(sourceSpec).toArray(new File[0]));
            }
            if (optionSet.has("p")) {
                config.setPort(optionSet.valueOf(portSpec));
            } else {
                config.setPort(39244);
            }
            config.setListen(optionSet.has("l"));
            if (optionSet.has("S")) {
                config.setScriptFile(optionSet.valueOf(scriptSpec));
            }
            if (optionSet.has("h")) {
                config.setHost(optionSet.valueOf(hostSpec));
            } else {
                config.setHost("localhost");
            }
            if (optionSet.has("j")) {
                List<File> files = new ArrayList<>(optionSet.valuesOf(jarSpec));
                // rt.jar
                File rtJarFile = new File(Thread.currentThread().getContextClassLoader().getResource("java/lang/String.class").getPath().replace("file:", "").split("!")[0]);
                files.add(rtJarFile);

                // javafx
                if (optionSet.has("f")) {
                    File parentFile = rtJarFile.getAbsoluteFile().getParentFile();
                    File[] childFiles = parentFile.listFiles();
                    if (!parentFile.isDirectory() || childFiles == null) {
                        throw new IllegalStateException("Wrong format of rt.jar's parent file...");
                    }
                    for (File childFile : childFiles) {
                        String name = childFile.getName();
                        if (!name.endsWith(".jar") || !name.startsWith("javafx")) {
                            continue;
                        }

                        files.add(childFile);
                    }
                }

                config.setJarResources(files.toArray(new File[0]));
            } else {
                throw new IllegalArgumentException();
            }
        } catch (OptionException | IllegalArgumentException e) {
            try {
                optionParser.printHelpOn(System.out);
            } catch (IOException ex) {
                System.err.println("Macchiato can not even print a manual...");
                ex.printStackTrace();
            }
            System.exit(1);
        }
    }
}
