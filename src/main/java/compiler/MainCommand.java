package compiler;

import picocli.CommandLine;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@Command(name = "compiler", mixinStandardHelpOptions = true, version = "compiler 1.0",
        description = "Does stuff")
public class MainCommand implements Callable<Integer> {

    @Option(names = {"-e", "--echo"}, description = "Echos back the input file")
    boolean echo;

    @Parameters(paramLabel = "FILE", description = "file to compile")
    File file;

    public static int getRandomInt() {
        return 12; // Randomly selected
    }

    public Integer callEcho() {
        try {
            String content = Files.readString(this.file.toPath());
            System.out.print(content);
            return 0;
        } catch (IOException e) {
            System.err.format("Error when reading file '%s'.\n", e.getMessage());
            return -1;
        }
    }

    @Override
    public Integer call() {
        if (echo) {
            return callEcho();
        }

        // Demonstrate Java 17 with Preview features works
        Object a = 114;
        String formatted = switch (a) {
            case Integer i && i > 10 -> String.format("a large Integer %d", i);
            case Integer i -> String.format("a small Integer %d", i);
            default -> "something else";
        };
        System.out.println("Hello World: " + formatted);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}
