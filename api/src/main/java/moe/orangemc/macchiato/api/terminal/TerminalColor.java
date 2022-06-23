package moe.orangemc.macchiato.cli;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public enum CliColor {
    WHITE(AttributedStyle.WHITE | AttributedStyle.BRIGHT),
    LIGHT_GRAY(AttributedStyle.WHITE),
    DARK_GRAY(AttributedStyle.BLACK | AttributedStyle.BRIGHT),
    BLACK(AttributedStyle.BLACK),
    DARK_RED(AttributedStyle.RED),
    RED(AttributedStyle.RED | AttributedStyle.BRIGHT),
    DARK_GREEN(AttributedStyle.GREEN),
    GREEN(AttributedStyle.GREEN | AttributedStyle.BRIGHT),
    GOLD(AttributedStyle.YELLOW),
    YELLOW(AttributedStyle.YELLOW | AttributedStyle.BRIGHT),
    BLUE(AttributedStyle.BLUE),
    AQUA(AttributedStyle.BLUE | AttributedStyle.BRIGHT),
    MAGENTA(AttributedStyle.MAGENTA),
    PINK(AttributedStyle.MAGENTA | AttributedStyle.BRIGHT),
    CYAN(AttributedStyle.CYAN),
    LIGHT_CYAN(AttributedStyle.CYAN | AttributedStyle.BRIGHT);
    private final int color;

    CliColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "\u00a7" + Integer.toHexString(getColor());
    }

    public static String toColorfulString(String from) {
        String[] slices = from.split("\u00a7");
        AttributedStringBuilder stringBuilder = new AttributedStringBuilder();

        stringBuilder.append(slices[0]);

        for (int i = 1; i < slices.length; i++) {
            String s = slices[i];
            if (s.length() <= 1) {
                continue;
            }

            AttributedStringBuilder subStringBuilder = new AttributedStringBuilder();
            int color = Integer.parseInt(s.substring(0, 1), 16);
            subStringBuilder.style(new AttributedStyle().foreground(color)).append(s.substring(1));
            stringBuilder.append(subStringBuilder);
        }

        return stringBuilder.toAnsi();
    }
}
