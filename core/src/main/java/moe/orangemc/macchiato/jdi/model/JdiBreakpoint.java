package moe.orangemc.macchiato.jdi.model;

import com.sun.jdi.request.BreakpointRequest;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Breakpoint {
    private final ClassNode owner;
    private final MethodNode method;
    private final int bytecodeIndex;

    private final BreakpointRequest breakpointRequest;

    private boolean deleted = false;

    public Breakpoint(ClassNode owner, MethodNode method, int bytecodeIndex, BreakpointRequest breakpointRequest) {
        this.owner = owner;
        this.method = method;
        this.bytecodeIndex = bytecodeIndex;
        this.breakpointRequest = breakpointRequest;

    }

    public ClassNode getOwner() {
        return owner;
    }

    public int getBytecodeIndex() {
        return bytecodeIndex;
    }

    public MethodNode getMethod() {
        return method;
    }

    public void delete() {
        if (deleted) {
            throw new IllegalStateException("This breakpoint is already deleted.");
        }

        breakpointRequest.virtualMachine().eventRequestManager().deleteEventRequest(breakpointRequest);
        deleted = true;
    }

    public boolean isEnabled() {
        if (deleted) {
            return false;
        }

        return breakpointRequest.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        if (deleted) {
            throw new IllegalStateException("This breakpoint is deleted.");
        }

        breakpointRequest.setEnabled(enabled);
    }
}
