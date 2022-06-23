package macchiato.jdi;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.Method;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.request.BreakpointRequest;
import macchiato.CoffeeConfiguration;
import macchiato.cli.CliManager;
import macchiato.jar.JarClassCache;
import macchiato.jdi.event.DebugBridgeEventLoop;
import macchiato.jdi.event.EventManager;
import macchiato.jdi.model.Breakpoint;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Map;

public class JdiDebugBridge {
    private VirtualMachine vm;
    private final EventManager eventManager = new EventManager();

    private final JdiCache cache;

    public JdiDebugBridge(CoffeeConfiguration config, JarClassCache jarClassCache, CliManager cliManager) throws BridgeFailureException {
        try {
            Class.forName("com.sun.jdi.Bootstrap");
        } catch (ClassNotFoundException e) {
            throw new BridgeFailureException("This version of JDK does not support JDI for the debug bridge.");
        }

        cache = new JdiCache(jarClassCache);

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        
        boolean succeed = false;

        if (config.isListen()) {
            cliManager.debugPrint("Listening on " + config.getHost() + ":" + config.getPort());

            for (ListeningConnector listeningConnector : virtualMachineManager.listeningConnectors()) {
                if (listeningConnector.transport().name().equals("dt_socket")) {
                    Map<String, Connector.Argument> args = listeningConnector.defaultArguments();

                    args.get("hostname").setValue(config.getHost());
                    args.get("port").setValue(String.valueOf(config.getPort()));

                    try {
                        vm = listeningConnector.accept(args);
                    } catch (IllegalConnectorArgumentsException e) {
                        throw new BridgeFailureException("Failed to listen from a debugee since the argument is wrong", e);
                    } catch (IOException e) {
                        throw new BridgeFailureException("Failed to listen from a debugee", e);
                    }

                    succeed = true;

                    break;
                }
            }

            if (!succeed) {
                throw new BridgeFailureException("Unable to find a fittest connector for your debuggee. Try removing -l option?");
            }
        } else {
            cliManager.debugPrint("Attaching to " + config.getHost() + ":" + config.getPort());

            for (AttachingConnector attachingConnector : virtualMachineManager.attachingConnectors()) {
                if (attachingConnector.transport().name().equals("dt_socket")) {
                    Map<String, Connector.Argument> args = attachingConnector.defaultArguments();

                    args.get("hostname").setValue(config.getHost());
                    args.get("port").setValue(String.valueOf(config.getPort()));

                    try {
                        vm = attachingConnector.attach(args);
                    } catch (IOException e) {
                        throw new BridgeFailureException("Failed to attach to the debugee.", e);
                    } catch (IllegalConnectorArgumentsException e) {
                        throw new BridgeFailureException("Failed to attach to a debugee since the argument is wrong", e);
                    }
                    succeed = true;

                    break;
                }
            }

            if (!succeed) {
                throw new BridgeFailureException("Unable to find a fittest connector for your debuggee. Try adding -l option?");
            }
        }

        new Thread(new DebugBridgeEventLoop(vm, eventManager, cliManager)).start();
    }

    public Breakpoint createBreakPoint(ClassNode targetClass, MethodNode targetMethod, int bytecodeIndex) {
        Method method = cache.findJdiMethod(targetMethod);

        BreakpointRequest bpr = vm.eventRequestManager().createBreakpointRequest(method.locationOfCodeIndex(bytecodeIndex));
        bpr.setEnabled(true);
        bpr.setSuspendPolicy(BreakpointRequest.SUSPEND_ALL);

        return new Breakpoint(targetClass, targetMethod, bytecodeIndex, bpr);
    }
}
