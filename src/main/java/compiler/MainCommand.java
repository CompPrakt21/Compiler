package compiler;

import picocli.CommandLine;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Model.CommandSpec;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.Callable;

@Command(name = "compiler", mixinStandardHelpOptions = true, version = "compiler 0.1.0",
        description = "MiniJava to x86 compiler")
public class MainCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Command(name = "--echo", description = "Echos back the input file.")
    public Integer callEcho(@Parameters(paramLabel = "FILE", description = "The file to echo.") File file) {
        try {
            String content = Files.readString(file.toPath());
            System.out.print(content);
            return 0;
        } catch (FileNotFoundException | NoSuchFileException e) {
            System.err.format("error: Can not find file: '%s'\n", file.getName());
            return -1;
        } catch (IOException e) {
            System.err.format("error: Can not read file: '%s'\n", file.getName());
            return -1;
        }
    }

    @Command(name = "--lextest", description = "Outputs lexed tokens for the input file")
    public Integer callLextest(@Parameters(paramLabel = "FILE", description = "The file to lxe.") File file) {
        boolean error = false;
        try {
            String content = Files.readString(file.toPath());
            Lexer l = new Lexer(content);
            loop:
            while (true) {
                Token t = l.nextToken();
                switch (t.type) {
                    case EOF -> {
                        System.out.println("EOF");
                        break loop;
                    }
                    case Error -> {
                        System.err.println("error: " + t.getErrorContent());
                        error = true;
                    }
                    case Identifier -> System.out.println("identifier " + t.getIdentContent());
                    case IntLiteral -> System.out.println("integer literal " + t.getIntLiteralContent());
                    default -> System.out.println(t.type.repr);
                }
            }
        } catch (FileNotFoundException | NoSuchFileException e) {
            System.err.format("error: Can not find file: '%s'\n", file.getName());
            error = true;
        } catch (IOException e) {
            System.err.format("error: Can not read file: '%s'\n", file.getName());
            error = true;
        }
        return error ? -1 : 0;
    }

    @Command(name = "--dump-dot-ast", description = "Generates a dot file with the ast.")
    public Integer callDumpAst(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        boolean error = false;

        try {
            String content = Files.readString(file.toPath());
            var parser = new Parser(new Lexer(content));
            var ast = parser.parse();
            parser.dotWriter(ast);
        } catch (FileNotFoundException | NoSuchFileException e) {
            System.err.format("error: Can not find file: '%s'\n", file.getName());
            error = true;
        } catch (IOException e) {
            System.err.format("error: Can not read file: '%s'\n", file.getName());
            error = true;
        }

        return error ? -1 : 0;
    }

    @Override
    public Integer call() {
        // For now, a subcommand is required.
        // Once compiling is implemented, that will be the default instead,
        throw new ParameterException(spec.commandLine(), "Specify a subcommand");
    }

    public static void main(String[] args) {
        // Debug picocli by uncommenting this:
        // System.setProperty("picocli.trace", "DEBUG");
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}
