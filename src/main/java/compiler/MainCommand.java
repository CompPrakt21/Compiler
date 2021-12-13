package compiler;

import compiler.ast.Program;
import compiler.codegen.FirmToLlir;
import compiler.codegen.llir.DumpLlir;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.semantic.ConstantFolding;
import compiler.semantic.WellFormed;
import compiler.semantic.resolution.NameResolution;
import compiler.syntax.Lexer;
import compiler.syntax.Parser;
import compiler.syntax.Token;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

@Command(name = "compiler", mixinStandardHelpOptions = true, version = "compiler 0.1.0",
        description = "MiniJava to x86 compiler")
public class MainCommand implements Callable<Integer> {

    @Parameters(paramLabel = "FILE", scope = CommandLine.ScopeType.INHERIT, description = "The file to operate on.")
    File file;

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

    @SuppressWarnings("unused")
    @Command(name = "--echo", description = "Echos back the input file.")
    public Integer callEcho() {
        return callWithFileContent(file, content -> {
            System.out.print(content);
            return false;
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--lextest", description = "Outputs lexed tokens for the input file")
    public Integer callLextest() {
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

    @SuppressWarnings("unused")
    @Command(name = "--parsetest", description = "Checks whether the input file parses.")
    public Integer callParseTest() {
        return callWithParsed(file, (reporter, parser, ast) -> false);
    }

    @SuppressWarnings("unused")
    @Command(name = "--dump-dot-ast", description = "Generates a dot file with the ast.")
    public Integer callDumpAst() {
        return callWithParsed(file, (reporter, parser, ast) -> {
            var resolution = NameResolution.performNameResolution(ast, reporter);

            var constantFolding = ConstantFolding.performConstantFolding(ast, Optional.of(reporter));

            var wellFormed = WellFormed.checkWellFormdness(ast, resolution, Optional.of(reporter));

            new DumpAst(new PrintWriter(System.out), resolution.definitions())
                    .addNodeAttribute("ty", resolution.expressionTypes())
                    .addNodeAttribute("const", constantFolding.constants())
                    .addNodeAttribute("local_vars", wellFormed.variableCounts())
                    .addNodeAttribute("dead", wellFormed.isDeadStatement())
                    .dump(ast);

            return !(resolution.successful() && wellFormed.correct() && constantFolding.successful());
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--print-ast", description = "Prett-prints the parsed AST.")
    public Integer printAst() {
        return callWithParsed(file, (reporter, parser, ast) -> {
            System.out.println(AstPrinter.program(ast));
            return false;
        });
    }

    @FunctionalInterface
    private interface PostCheckOperation {
        boolean run(CompilerMessageReporter r, FrontendResult result);
    }

    private static Integer callWithChecked(File file, PostCheckOperation op) {
        return callWithParsed(file, (reporter, parser, ast) -> {
            var nameResolutionResult = NameResolution.performNameResolution(ast, reporter);

            var constantFolding = ConstantFolding.performConstantFolding(ast, Optional.of(reporter));

            var wellFormed = WellFormed.checkWellFormdness(ast, nameResolutionResult, Optional.of(reporter));

            var result = new FrontendResult(
                    file,
                    ast,
                    nameResolutionResult.definitions(),
                    nameResolutionResult.expressionTypes(),
                    nameResolutionResult.bindingTypes(),
                    nameResolutionResult.classes(),
                    constantFolding.constants(),
                    wellFormed.variableCounts(),
                    wellFormed.isDeadStatement(),
                    wellFormed.mainMethod()
            );

            if (nameResolutionResult.successful() && wellFormed.correct() && constantFolding.successful()) {
                return op.run(reporter, result);
            } else {
                return true;
            }
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--check", description = "Performs semantic analysis of the input.")
    public Integer check() {
        return callWithChecked(file, (reporter, result) -> false);
    }

    @SuppressWarnings("unused")
    @Command(name = "--firm-version", description = "Print Firm version.")
    public Integer firmVersion() {
        FirmTest.printVersion();
        return 0;
    }

    @SuppressWarnings("unused")
    @Command(name = "--compile-firm", description = "Compile to binary.")
    public Integer translate(@Option(names = "--dump", description = "Dump the resulting FIRM graphs.") boolean dumpGraphs) {
        return callWithChecked(file, (reporter, frontend) -> {
            File runtimePath = null;
            try {
                var jarFile = new File(MainCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                var baseDir = jarFile.getParentFile().getParentFile();
                runtimePath = new File(baseDir, "libruntime.c");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return true;
            }

            var asmOutput = new File(file.getName() + ".s");
            FirmBackend backend = new FirmBackend(asmOutput, runtimePath, frontend);
            backend.generateASM(dumpGraphs);

            return false;
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--backend", description = "Compile to binary.")
    public Integer backend() {
        return callWithChecked(file, (reporter, frontend) -> {

            var translationResult = new Translation(frontend).translate(false);

            var graph = FirmToLlir.lowerFirm(translationResult);

            try {
                new DumpLlir(new PrintWriter(new File("bb_out.dot"))).dump(graph);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return false;
        });
    }


    @Override
    public Integer call() {
        return translate(false);
    }

    public static void main(String[] args) {
        // Debug picocli by uncommenting this:
        // System.setProperty("picocli.trace", "DEBUG");
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}
