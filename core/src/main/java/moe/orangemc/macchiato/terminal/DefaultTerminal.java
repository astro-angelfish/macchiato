package moe.orangemc.macchiato.cli;

import moe.orangemc.macchiato.CoffeeConfiguration;
import moe.orangemc.macchiato.api.terminal.TerminalColor;
import moe.orangemc.macchiato.api.terminal.WrappedTerminal;
import moe.orangemc.macchiato.cli.command.CommandMap;
import org.jline.builtins.Completers;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.*;
import java.util.List;

public class DefaultCli implements WrappedTerminal {
    private final Terminal terminal;
    private final LineReader lineReader;
    private final CommandMap commandMap;
    private boolean verbose;

    public DefaultCli(CoffeeConfiguration config, CommandMap commandMap) throws CliFailureException {
        Terminal generalTerminal;
        try {
            generalTerminal = TerminalBuilder.builder().system(true).jansi(true).color(true).name("Macchiato Terminal").build();
        } catch (IOException e) {
            throw new CliFailureException("Unable to initialize the main terminal", e);
        }

        this.terminal = generalTerminal;
        this.verbose = config.isVerbose();

        if (config.getScriptFile() != null) {
            debugPrint("Loading and executing script file...");

            if (!config.getScriptFile().exists()) {
                throw new CliFailureException("Script file does not found!!!", new FileNotFoundException(config.getScriptFile().getAbsolutePath()));
            }

            // Load the script file and execute it.
            try (FileInputStream fis = new FileInputStream(config.getScriptFile()); Terminal fileTerminal = TerminalBuilder.builder()
                    .system(false)
                    .streams(fis, System.out)
                    .color(true)
                    .name("Macchiato File Reader")
                    .build()) {
                LineReader lineReader = LineReaderBuilder.builder()
                        .terminal(fileTerminal)
                        .parser(new DefaultParser()
                                .eofOnEscapedNewLine(true)
                                .eofOnUnclosedBracket(DefaultParser.Bracket.CURLY, DefaultParser.Bracket.SQUARE, DefaultParser.Bracket.ROUND)
                                .eofOnUnclosedQuote(true)
                        )
                        .build();
                while (true) {
                    this.performCommand(lineReader.readLine(), true);
                }
            } catch (EndOfFileException e) {
                debugPrint("Macchiato has finished executing script file.");
            } catch (UserInterruptException e) {
                println(TerminalColor.RED + "You have interrupted the script execution.");
            } catch (IOException e) {
                generalTerminal.writer().println(TerminalColor.toColorfulString(TerminalColor.RED + "Macchiato cannot read line from the script file: " + config.getScriptFile()));
                printException(e);
            }
        }

        lineReader = LineReaderBuilder.builder()
                .terminal(generalTerminal)
                .parser(new DefaultParser()
                        .eofOnUnclosedBracket(DefaultParser.Bracket.CURLY, DefaultParser.Bracket.SQUARE, DefaultParser.Bracket.ROUND)
                        .eofOnUnclosedQuote(true)
                        .eofOnEscapedNewLine(true)
                )
                .completer(new Completer() {
                    private final Completers.FileNameCompleter fileNameCompleter = new Completers.FileNameCompleter();
                    private final StringsCompleter stringsCompleter = new StringsCompleter();
                    @Override
                    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
                        stringsCompleter.complete(lineReader, parsedLine, list);
                        fileNameCompleter.complete(lineReader, parsedLine, list);
                    }
                })
                .build();


        this.commandMap = commandMap;
    }

    @Override
    public void printException(Throwable t) {
        printException(t, TerminalColor.RED);
    }

    @Override
    public void printException(Throwable t, TerminalColor colorType) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String ansi = new AttributedStringBuilder().style(new AttributedStyle().foreground(colorType.getColor())).append(sw.toString()).toAnsi();
        terminal.writer().println(ansi);
        terminal.writer().flush();
    }

    @Override
    public void println(String str) {
        terminal.writer().println(TerminalColor.toColorfulString(str));
        terminal.writer().flush();
    }

    @Override
    public void debugPrint(String str) {
        if (verbose) {
            terminal.writer().println(TerminalColor.toColorfulString(TerminalColor.GREEN + str));
        }
    }

    private void performCommand(String line) {
        performCommand(line, false);
    }
    private void performCommand(String line, boolean skipEmpty) {
        if ((!skipEmpty) && line.trim().isEmpty()) {
            History history = lineReader.getHistory();
            while (history.previous()) {
                String current = history.current();
                if (current.trim().length() > 0) {
                    line = current;
                    break;
                }
            }
        }
        String[] parsedLine = line.split("(?<!\\\\) ");
        String cmd = parsedLine[0];
        String[] parsedArgs = new String[parsedLine.length - 1];
        System.arraycopy(parsedLine, 1, parsedArgs, 0, parsedArgs.length);
        commandMap.executeCommand(this, cmd, parsedArgs);
    }

    @Override
    public int readInt(String prompt) {
        println("");
        String s = lineReader.readLine(TerminalColor.toColorfulString(prompt + TerminalColor.WHITE));
        return Integer.parseInt(s);
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void startReading() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    String line = lineReader.readLine(System.getProperty("user.name") + "@macchiato$ ");
                    performCommand(line);
                } catch (EndOfFileException e) {
                    println("Got EOF while interactive.");
                    System.exit(0);
                } catch (UserInterruptException e) {
                    println("Use Ctrl-D or quit/exit command to let macchiato go.");
                }
            }
        }, "Console Handler");
        t.setDaemon(true);
        t.start();
    }
}
