package compiler;

import compiler.ast.Class;
import compiler.ast.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstPrinter {

    // Important ERROR related invariants ensured by the parser:
    // - Optionals will never be null
    // - Lists will never be null
    private final static String error = "<ERROR>";

    private static String lines(List<String> ls) {
        ls = ls.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
        return String.join("\n", ls);
    }

    private static String lines(String... ls) {
        return lines(Arrays.stream(ls).toList());
    }

    private static String indent(String s) {
        if (s.equals("")) return "";
        return Arrays.stream(s.split("\n"))
                .map(line -> "\t" + line)
                .collect(Collectors.joining("\n"));
    }

    private static <T, U extends Comparable<? super U>> List<T> sortedBy(List<? extends T> xs, Function<? super T, ? extends U> f) {
        Stream<? extends T> nulls = xs.stream().filter(Objects::isNull);
        Stream<? extends T> sorted = xs.stream().filter(Objects::nonNull).sorted(Comparator.comparing(f));
        return Stream.concat(nulls, sorted).collect(Collectors.toList());
    }

    private static List<String> all(List<? extends AstNode> xs) {
        return xs.stream().map(AstPrinter::print).collect(Collectors.toList());
    }

    private static String fmt(String format, Object... args) {
        var array = new Object[args.length];

        // Sorry Marc, but this doesn't take three stack frames.
        for (int i = 0; i < array.length; i++) {
            if (args[i] == null) {
                array[i] = error;
            } else if (args[i] instanceof AstNode a) {
                array[i] = print(a);
            } else {
                array[i] = args[i];
            }
        }

        return String.format(format, array);
    }

    public static String type(Type t) {
        StringBuilder arraySuffix = new StringBuilder();
        while (t instanceof ArrayType a) {
            arraySuffix.append("[]");
            t = a.getChildType();
        }
        return switch (t) {
            case null -> error;
            case VoidType ignored -> "void";
            case BoolType ignored -> "boolean";
            case IntType ignored -> "int";
            case ClassType c -> print(c.getIdentifier());
            case ArrayType ignored -> throw new AssertionError();
        } + arraySuffix;
    }

    public static String parameter(Parameter p) {
        return fmt("%s %s", p.getType(), p.getIdentifier());
    }

    public static String expressionTopLevel(Expression e) {
        if (e == null) return error;
        return switch (e) {
            case AssignmentExpression a -> fmt("%s = %s", a.getLvalue(), a.getRvalue());
            case BinaryOpExpression b -> fmt("%s %s %s", b.getLhs(), b.getOperatorRepr(), b.getRhs());
            case UnaryExpression u -> {
                String gap = u.getExpression() instanceof IntLiteral i && i.getMinusToken().isPresent() ? " " : "";
                yield fmt("%s%s%s", u.getOperatorRepr(), gap, u.getExpression());
            }
            case MethodCallExpression m -> {
                String targetPrefix = m.getTarget().map(t -> fmt("%s.", t)).orElse("");
                String args = m.getArguments().stream()
                        .map(AstPrinter::expressionTopLevel)
                        .collect(Collectors.joining(", "));
                yield fmt("%s%s(%s)", targetPrefix, m.getIdentifier(), args);
            }
            case FieldAccessExpression f -> fmt("%s.%s", f.getTarget(), f.getIdentifier());
            case ArrayAccessExpression a -> fmt("%s[%s]", a.getTarget(), expressionTopLevel(a.getIndexExpression()));
            case BoolLiteral b -> String.valueOf(b.getValue());
            case IntLiteral i -> print(i.getMinusToken().map(ignored -> "-").orElse("") + i.getValue());
            case ThisExpression ignored -> "this";
            case NewObjectExpression n -> fmt("new %s()", n.getType().getIdentifier());
            case NewArrayExpression n -> {
                String dimensionBrackets = "[]".repeat(n.getDimensions() - 1);
                yield fmt("new %s[%s]%s", n.getType(), expressionTopLevel(n.getFirstDimensionSize()), dimensionBrackets);
            }
            case Reference r -> print(r.getIdentifier());
            case NullExpression ignored -> "null";
        };
    }

    public static String expression(Expression e) {
        String p = expressionTopLevel(e);
        if (e instanceof IntLiteral || e instanceof BoolLiteral || e instanceof NullExpression
                || e instanceof ThisExpression || e instanceof Reference) {
            return p;
        }
        return fmt("(%s)", p);
    }

    private static String subStatement(Statement s) {
        String printed = print(s);
        if (s instanceof Block || s instanceof EmptyStatement) {
            return " " + printed;
        }
        return "\n" + indent(printed);
    }

    private static String ifStatement(IfStatement i) {
        Statement t = i.getThenBody();
        String thenBody = subStatement(t);
        String elseBody = i.getElseBody().map(b -> {
            String prefix = t instanceof Block || t instanceof EmptyStatement ? " " : "\n";
            String printed = print(b);
            printed = b instanceof Block || b instanceof EmptyStatement || b instanceof IfStatement
                    ? " " + printed
                    : "\n" + indent(printed);
            return prefix + "else" + printed;
        }).orElse("");
        return fmt("if (%s)%s%s", expressionTopLevel(i.getCondition()), thenBody, elseBody);
    }

    private static String block(Block b) {
        List<Statement> statements = b.getStatements().stream()
                .filter(s -> !(s instanceof EmptyStatement))
                .collect(Collectors.toList());
        if (statements.isEmpty()) {
            return "{ }";
        }
        String formattedStatements = indent(lines(all(statements)));
        return lines(
                "{",
                formattedStatements,
                "}");
    }

    public static String statement(Statement s) {
        if (s == null) return error;
        return switch (s) {
            case Block b -> block(b);
            case EmptyStatement ignored -> "{ }";
            case IfStatement i -> ifStatement(i);
            case ExpressionStatement e -> expressionTopLevel(e.getExpression()) + ";";
            case WhileStatement w -> fmt("while (%s)%s", expressionTopLevel(w.getCondition()), subStatement(w.getBody()));
            case ReturnStatement r -> fmt("return%s;", r.getExpression().map(e -> " " + expressionTopLevel(e)).orElse(""));
            case LocalVariableDeclarationStatement l -> {
                String initializer = l.getInitializer().map(e -> " = " + expressionTopLevel(e)).orElse("");
                yield fmt("%s %s%s;", l.getType(), l.getIdentifier(), initializer);
            }
        };
    }

    public static String method(Method m) {
        String staticKeyword = m.isStatic() ? " static" : "";
        String parameters = String.join(", ", all(m.getParameters()));
        return fmt("public%s %s %s(%s) %s",
                staticKeyword, m.getReturnType(), m.getIdentifier(), parameters, m.getBody());
    }

    public static String field(Field f) {
        return fmt("public %s %s;", f.getType(), f.getIdentifier());
    }

    public static String _class(Class c) {
        // Methods and fields are sorted alphabetically
        List<Method> methods = sortedBy(c.getMethods(), method -> method.getIdentifier().getContent());
        List<Field> fields = sortedBy(c.getFields(), field -> field.getIdentifier().getContent());
        String printedMethods = indent(lines(all(methods)));
        String printedFields = indent(lines(all(fields)));
        return lines(
                fmt("class %s {", c.getIdentifier()),
                printedMethods,
                printedFields,
                "}");
    }

    public static String program(Program p) {
        // Classes are sorted alphabetically
        List<Class> classes = sortedBy(p.getClasses(), klass -> klass.getIdentifier().getContent());
        return lines(all(classes));
    }

    public static String print(AstNode a) {
        if (a == null) return error;
        return switch (a) {
            case Expression e -> expression(e);
            case Statement s -> statement(s);
            case Type t -> type(t);
            case Program p -> program(p);
            case Class c -> _class(c);
            case Method m -> method(m);
            case Field f -> field(f);
            case Parameter p -> parameter(p);
            case Identifier i -> print(i.getContent());
        };
    }

    public static String print(String s) {
        if (s == null) return error;
        return s;
    }

}
