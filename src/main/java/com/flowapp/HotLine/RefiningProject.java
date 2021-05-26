package com.flowapp.HotLine;

import com.flowapp.HotLine.Utils.FileUtils;
import de.vandermeer.asciitable.AsciiTable;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefiningProject {

    public static void main(String[] args) {
        FileUtils.clear();
    }

    private static void renderTable(Object[] ... args) {
        AsciiTable at = new AsciiTable();
        at.addRule();
        for (var row: args) {
            at.addRow(row);
            at.addRule();
        }
        String rend = at.render();
        println(rend);
    }

    private static void println(@NotNull String pattern, Object... args) {
        final String message = format(pattern, args);
        System.out.println(message);
        FileUtils.printOut(message);
    }

    @NotNull
    private static String format(@NotNull String pattern, Object... args) {
        Pattern rePattern = Pattern.compile("\\{([0-9+-]*)}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = rePattern.matcher(pattern);
        int counter = -1;
        while (matcher.find()) {
            counter++;
            String number = matcher.group(1);
            if (number == null) {
                number = "";
            }
            if (!number.isBlank()) {
                if (number.equals("+")) {
                    number = "\\+";
                    counter++;
                } else if (number.equals("-")) {
                    counter--;
                } else {
                    counter = Integer.parseInt(number);
                }
            }
            counter = clamp(counter, 0, args.length - 1);
            String toChange = "\\{" + number + "}";
            String result = "{" + counter + "}";
            pattern = pattern.replaceFirst(toChange, result);
        }
        return MessageFormat.format(pattern, args);
    }

    private static <T extends Comparable<T>> T clamp(T val, T min, T max) {
        if (val.compareTo(min) < 0) return min;
        else if (val.compareTo(max) > 0) return max;
        else return val;
    }
}
