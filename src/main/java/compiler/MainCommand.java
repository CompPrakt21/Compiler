package compiler;

import compiler.ast.Program;
import compiler.diagnostics.CompilerMessageReporter;
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
import java.util.function.Function;

@Command(name = "compiler", mixinStandardHelpOptions = true, version = "compiler 0.1.0",
        description = "MiniJava to x86 compiler")
public class MainCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    private static int callWithFileContent(File file, Function<String, Boolean> f) {
        try {
            String content = Files.readString(file.toPath());
            boolean error = f.apply(content);
            return error ? 1 : 0;
        } catch (FileNotFoundException | NoSuchFileException e) {
            System.err.format("error: Can not find file: '%s'\n", file.getName());
            return 1;
        } catch (IOException e) {
            System.err.format("error: Can not read file: '%s'\n", file.getName());
            return 1;
        }
    }

    @Command(name = "--echo", description = "Echos back the input file.")
    public Integer callEcho(@Parameters(paramLabel = "FILE", description = "The file to echo.") File file) {
        return callWithFileContent(file, content -> {
            System.out.print(content);
            return false;
        });
    }

    @Command(name = "--lextest", description = "Outputs lexed tokens for the input file")
    public Integer callLextest(@Parameters(paramLabel = "FILE", description = "The file to lxe.") File file) {
        return callWithFileContent(file, content -> {
            Lexer l = new Lexer(content);
            boolean error = false;
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
            return error;
        });
    }

    @FunctionalInterface
    private interface PostParseOperation {
        boolean run(CompilerMessageReporter r, Parser p, Program prog);
    }

    private static Integer callWithParsed(File file, PostParseOperation op) {
        return callWithFileContent(file, content -> {
            var reporter = new CompilerMessageReporter(new PrintWriter(System.err), content);
            var parser = new Parser(new Lexer(content), reporter);
            var ast = parser.parse();
            if (!parser.successfulParse) {
                reporter.finish();
                return true;
            }
            boolean error = op.run(reporter, parser, ast);
            reporter.finish();
            return error;
        });
    }

    @Command(name = "--parsetest", description = "Checks whether the input file parses.")
    public Integer callParseTest(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithParsed(file, (reporter, parser, ast) -> false);
    }

    @Command(name = "--dump-dot-ast", description = "Generates a dot file with the ast.")
    public Integer callDumpAst(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithParsed(file, (reporter, parser, ast) -> {
            parser.dotWriter(ast);
            return false;
        });
    }

    @Command(name = "--print-ast", description = "Prett-prints the parsed AST.")
    public Integer printAst(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithParsed(file, (reporter, parser, ast) -> {
            System.out.println(AstPrinter.program(ast));
            return false;
        });
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
