package macchiato.jdi.event;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import macchiato.cli.CliColor;
import macchiato.cli.CliManager;

public class DebugBridgeEventLoop implements Runnable {
    private final VirtualMachine vm;
    private final EventManager eventManager;
    private final CliManager cliManager;

    public DebugBridgeEventLoop(VirtualMachine vm, EventManager eventManager, CliManager cliManager) {
        this.vm = vm;
        this.eventManager = eventManager;
        this.cliManager = cliManager;
    }

    @Override
    public void run() {
        Thread.currentThread().setDaemon(true);
        Thread.currentThread().setName("Debug Bridge Event Loop Thread");

        while (true) {
            EventSet events;
            try {
                events = vm.eventQueue().remove();
                for (Event event : events) {
                    eventManager.callEvent(event);
                }
            } catch (InterruptedException e) {
                cliManager.println(CliColor.GREEN + "Interrupted in event loop. But Macchiato decided to ignore this interruption");
            }
        }
    }
}
