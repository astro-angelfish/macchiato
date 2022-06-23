package macchiato;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import macchiato.cli.CliColor;
import macchiato.cli.CliFailureException;
import macchiato.cli.CliManager;
import macchiato.jdi.BridgeFailureException;
import macchiato.jdi.model.Breakpoint;
import macchiato.util.Stringifier;
import macchiato.cli.command.CommandMap;
import macchiato.jar.JarClassCache;
import macchiato.jdi.JdiDebugBridge;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class MacchiatoDebugger {
    private static MacchiatoDebugger instance;

    private int numLoggedBreakpoints = 0;
    private final BiMap<Integer, Breakpoint> breakpoints = HashBiMap.create();

    private final CommandMap commandMap = new CommandMap();
    
    private final CliManager cliManager;
    private final JdiDebugBridge debugBridge;
    private final JarClassCache jarClassCache;

    public MacchiatoDebugger(CoffeeConfiguration configuration) {
        try {
            cliManager = new CliManager(configuration, commandMap);
        } catch (CliFailureException e) {
            System.err.println(CliColor.toColorfulString(CliColor.RED + "Macchiato could not initialize terminal."));
            e.printStackTrace();
            System.exit(-1);
            throw new RuntimeException("Macchiato should not reach here.");
        }
        
        cliManager.debugPrint("Terminal handler loaded.");
        jarClassCache = new JarClassCache(configuration, cliManager);
        cliManager.debugPrint("Statically class cache loaded.");

        try {
            debugBridge = new JdiDebugBridge(configuration, jarClassCache, cliManager);
        } catch (BridgeFailureException e) {
            System.err.println(CliColor.toColorfulString(CliColor.RED + "Macchiato could not initialize the debug bridge."));
            e.printStackTrace();
            System.exit(-2);
            throw new RuntimeException("Macchiato should not reach here.");
        }
        cliManager.debugPrint("Debug bridge loaded.");

        instance = this;
    }
    
    public void addBreakpoint(String className, String methodName, int bytecodeIndex) {
        // Step 1: find the class.
        List<ClassNode> foundClasses = jarClassCache.findClass(className);

        if (foundClasses.size() == 0) {
            cliManager.println(CliColor.RED + "No class with name: " + CliColor.GOLD + className + CliColor.RED + " found in the jar definitions.");
            return;
        }

        ClassNode chosenClass = foundClasses.get(0);

        if (foundClasses.size() > 1) {
            cliManager.println(CliColor.GREEN + "Macchiato has found " + CliColor.GOLD + foundClasses.size() + CliColor.GREEN + " classes, which one is the class you want to print breakpoint on?");

            for (int i = 0; i < foundClasses.size(); i++) {
                cliManager.println(i + ": " + Stringifier.stringifyClass(foundClasses.get(i)));
            }

            try {
                int chosenClassIndex = cliManager.readInt("Choose a class or type any other thing to abort: ");
                chosenClass = foundClasses.get(chosenClassIndex);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                cliManager.println(CliColor.YELLOW + "Ok. Macchiato has stopped this operation");
                return;
            }
        }

        // Step 2: find the method.
        List<MethodNode> methods = chosenClass.methods.stream().filter((m) -> m.name.equals(methodName)).toList();
        if (methods.size() == 0) {
            cliManager.println(CliColor.RED + "Macchiato didn't find any method called " + methodName + " in the class...");
            return;
        }

        MethodNode chosenMethod = methods.get(0);
        if (methods.size() > 1) {
            cliManager.println(CliColor.GREEN + "Macchiato has found " + CliColor.GOLD + methods.size() + " methods called " + methodName + ", which one is the method you want to place the breakpoint on?");

            for (int i = 0; i < methods.size(); i++) {
                cliManager.println(i + ": " + Stringifier.stringifyMethod(methods.get(i)));
            }

            try {
                int chosenMethodIndex = cliManager.readInt("Choose a method or type any other to abort: ");
                chosenMethod = methods.get(chosenMethodIndex);
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                cliManager.println(CliColor.YELLOW + "Ok. Macchiato has stopped this operation");
                return;
            }
        }

        // Step 3: Place down the breakpoint.
        cliManager.println("Ok. Breakpoint#" + numLoggedBreakpoints + " has placed down.");
        breakpoints.put(numLoggedBreakpoints ++, debugBridge.createBreakPoint(chosenClass, chosenMethod, bytecodeIndex));
    }

    public static MacchiatoDebugger getInstance() {
        return instance;
    }

    public CliManager getCliManager() {
        return cliManager;
    }

    public JdiDebugBridge getDebugBridge() {
        return debugBridge;
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }
}
