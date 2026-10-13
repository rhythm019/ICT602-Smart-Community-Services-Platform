package com.greenfield.scsp.presentation;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Thin wrapper around console input and output. Every screen uses it, so
 * prompts, validation loops and formatting look the same everywhere.
 */
public class ConsoleIO {

    private final Scanner in;
    private final PrintStream out;

    /**
     * @param in  source of user input
     * @param out destination for output
     */
    public ConsoleIO(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Prints a line. */
    public void println(String text) {
        out.println(text);
    }

    /** Prints a boxed screen heading. */
    public void heading(String title) {
        String bar = "=".repeat(Math.max(40, title.length() + 4));
        out.println();
        out.println(bar);
        out.println("  " + title);
        out.println(bar);
    }

    /** Prints a success message. */
    public void success(String text) {
        out.println("  [OK] " + text);
    }

    /** Prints an error message. */
    public void error(String text) {
        out.println("  [!] " + text);
    }

    /**
     * Reads a line of text.
     *
     * @param prompt prompt to show
     * @return trimmed input, or an empty string at end of input
     */
    public String ask(String prompt) {
        out.print("  " + prompt + ": ");
        out.flush();
        if (!in.hasNextLine()) {
            return "";
        }
        return in.nextLine().trim();
    }

    /**
     * Reads a password. Input is read through the same Scanner as other
     * prompts so scripted demos and tests work; a production web client
     * would mask this field.
     *
     * @param prompt prompt to show
     * @return entered password (not trimmed of inner spaces)
     */
    public String askPassword(String prompt) {
        return ask(prompt);
    }

    /**
     * Shows a numbered menu and returns the chosen option (1-based).
     * Keeps asking until a valid number is entered.
     *
     * @param options menu labels
     * @return chosen option number, or 0 at end of input
     */
    public int menu(List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            out.printf("  %d. %s%n", i + 1, options.get(i));
        }
        while (true) {
            // Show the prompt first so the user knows the program is waiting.
            out.print("  Choose an option: ");
            out.flush();
            if (!in.hasNextLine()) {
                out.println();
                return 0;
            }
            String line = in.nextLine().trim();
            try {
                int choice = Integer.parseInt(line);
                if (choice >= 1 && choice <= options.size()) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            error("Please enter a number from 1 to " + options.size() + ".");
        }
    }

    /**
     * Reads a whole number within a range.
     *
     * @param prompt prompt to show
     * @param min    smallest allowed value
     * @param max    largest allowed value
     * @return the number entered
     */
    public int askInt(String prompt, int min, int max) {
        while (true) {
            String line = ask(prompt + " (" + min + "-" + max + ")");
            if (line.isEmpty() && !in.hasNextLine()) {
                return min;
            }
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the error message
            }
            error("Enter a number from " + min + " to " + max + ".");
        }
    }
}
