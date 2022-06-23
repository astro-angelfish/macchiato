package moe.orangemc.macchiato.cli;

import moe.orangemc.macchiato.api.terminal.TerminalColor;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;

public class StartupCli implements WrappedTerminal {
    @Override
    public void printException(Throwable t) {

    }

    @Override
    public void printException(Throwable t, TerminalColor colorType) {

    }

    @Override
    public void println(String str) {

    }

    @Override
    public void debugPrint(String str) {

    }

    @Override
    public int readInt(String prompt) {
        return 0;
    }

    @Override
    public void setVerbose(boolean verbose) {

    }

    @Override
    public void startReading() {

    }
}
