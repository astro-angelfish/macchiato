package moe.orangemc.macchiato.api.terminal;

public interface TerminalManager {
    void printException(Throwable t);

    void printException(Throwable t, TerminalColor colorType);

    void println(String str);

    void debugPrint(String str);

    int readInt(String prompt);

    void setVerbose(boolean verbose);

    void startReading();
}
