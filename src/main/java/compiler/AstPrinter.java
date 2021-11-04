package compiler;

import compiler.ast.*;
import compiler.ast.Class;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AstPrinter {

    private static String lines(String... ls) {
        return String.join("\n", ls);
    }

    private static String lines(List<String> ls) {
        return String.join("\n", ls);
    }

    private static String indent(String s) {
        return Arrays.stream(s.split("\n"))
                .map(line -> "\t" + line)
                .collect(Collectors.joining("\n"));
    }

    private static <T, U extends Comparable<? super U>> List<T> sortedBy(List<? extends T> xs, Function<? super T, ? extends U> f) {
        return xs.stream().sorted(Comparator.comparing(f)).collect(Collectors.toList());
    }

    private static <T> List<String> prettyPrintAll(List<? extends T> xs, Function<? super T, String> prettyPrint) {
        return xs.stream().map(prettyPrint).collect(Collectors.toList());
    }

    public static String prettyPrint(Type t) {
        StringBuilder arraySuffix = new StringBuilder();
        while (t instanceof ArrayType a) {
            arraySuffix.append("[]");
            t = a.getChildType();
        }
        return switch (t) {
            case VoidType v -> "void";
            case BoolType b -> "boolean";
            case IntType i -> "int";
            case ClassType c -> c.getIdentifier();
            case ArrayType a -> throw new AssertionError();
        } + arraySuffix;
    }

    public static String prettyPrint(Parameter p) {
        return String.format("%s %s", prettyPrint(p.getType()), p.getIdentifier());
    }

    public static String prettyPrint(AssignmentExpression a) {
        return String.format("%s = %s", prettyPrint(a.getLvalue()), prettyPrint(a.getRvalue()));
    }

    public static String prettyPrint(BinaryOpExpression b) {
        return String.format("%s %s %s", prettyPrint(b.getLhs()), b.getOperatorRepr(), prettyPrint(b.getRhs()));
    }

    public static String prettyPrint(UnaryExpression u) {
        return String.format("%s%s", u.getOperatorRepr(), prettyPrint(u.getExpression()));
    }

    public static String prettyPrint(MethodCallExpression m) {
        String targetPrefix = m.getTarget().map(e -> prettyPrint(e) + ".").orElse("");
        String arguments = String.join(", ", prettyPrintAll(m.getArguments(), AstPrinter::prettyPrintTopLevel));
        return String.format("%s%s(%s)", targetPrefix, m.getIdentifier(), arguments);
    }

    public static String prettyPrint(FieldAccessExpression e) {
        return String.format("%s.%s", prettyPrint(e.getTarget()), e.getIdentifier());
    }

    public static String prettyPrint(ArrayAccessExpression a) {
        return String.format("%s[%s]", prettyPrint(a.getTarget()), prettyPrintTopLevel(a.getIndexExpression()));
    }

    public static String prettyPrint(NewObjectExpression n) {
        return String.format("new %s()", n.getTypeIdentifier());
    }

    public static String prettyPrint(NewArrayExpression n) {
        String type = prettyPrint(n.getType());
        String firstDimensionSize = prettyPrintTopLevel(n.getFirstDimensionSize());
        String dimensionBrackets = "[]".repeat(n.getDimensions() - 1);
        return String.format("new %s[%s]%s", type, firstDimensionSize, dimensionBrackets);
    }

    public static String prettyPrintTopLevel(Expression e) {
        return switch (e) {
            case AssignmentExpression a -> prettyPrint(a);
            case BinaryOpExpression b -> prettyPrint(b);
            case UnaryExpression u -> prettyPrint(u);
            case MethodCallExpression m -> prettyPrint(m);
            case FieldAccessExpression f -> prettyPrint(f);
            case ArrayAccessExpression a -> prettyPrint(a);
            case BoolLiteral b -> String.valueOf(b.getValue());
            case IntLiteral i -> i.getValue();
            case ThisExpression t -> "this";
            case NewObjectExpression n -> prettyPrint(n);
            case NewArrayExpression n -> prettyPrint(n);
            case Reference r -> r.getIdentifier();
            case NullExpression n -> "null";
        };
    }

    public static String prettyPrint(Expression e) {
        String p = prettyPrintTopLevel(e);
        if (e instanceof IntLiteral || e instanceof BoolLiteral || e instanceof NullExpression
                || e instanceof ThisExpression || e instanceof Reference) {
            return p;
        }
        return String.format("(%s)", p);
    }

    private static String subStatement(Statement s) {
        String printed = prettyPrint(s);
        if (s instanceof Block) {
            return " " + printed;
        }
        return "\n" + indent(printed);
    }

    public static String prettyPrint(IfStatement i) {
        String thenBody = subStatement(i.getThenBody());
        String elseBody = i.getElseBody().map(b -> "\nelse" + subStatement(b)).orElse("");
        return String.format("if (%s)%s%s", prettyPrintTopLevel(i.getCondition()), thenBody, elseBody);
    }

    public static String prettyPrint(WhileStatement w) {
        return String.format("while (%s)%s", prettyPrintTopLevel(w.getCondition()), subStatement(w.getBody()));
    }

    public static String prettyPrint(ReturnStatement r) {
        return String.format("return%s;", r.getExpression().map(AstPrinter::prettyPrintTopLevel).orElse(""));
    }

    public static String prettyPrint(LocalVariableDeclarationStatement l) {
        String initializer = l.getInitializer().map(e -> " = " + prettyPrintTopLevel(e)).orElse("");
        return String.format("%s %s%s;", prettyPrint(l.getType()), l.getIdentifier(), initializer);
    }

    public static String prettyPrint(Statement s) {
        return switch (s) {
            case Block b -> prettyPrint(b);
            case EmptyStatement e -> ";";
            case IfStatement i -> prettyPrint(i);
            case ExpressionStatement e -> prettyPrintTopLevel(e.getExpression()) + ";";
            case WhileStatement w -> prettyPrint(w);
            case ReturnStatement r -> prettyPrint(r);
            case LocalVariableDeclarationStatement l -> prettyPrint(l);
        };
    }

    public static String prettyPrint(Block b) {
        List<Statement> statements = b.getStatements().stream()
                .filter(s -> !(s instanceof EmptyStatement))
                .collect(Collectors.toList());
        if (statements.isEmpty()) {
            return "{ }";
        }
        String formattedStatements = indent(lines(prettyPrintAll(statements, AstPrinter::prettyPrint)));
        return String.format(lines(
                "{",
                "%s",
                "}"
        ), formattedStatements);
    }

    public static String prettyPrint(Method m) {
        String staticKeyword = m.isStatic() ? "static" : "";
        String type = prettyPrint(m.getReturnType());
        String parameters = String.join(", ", prettyPrintAll(m.getParameters(), AstPrinter::prettyPrint));
        String body = prettyPrint(m.getBody());
        return String.format("public%s %s %s(%s) %s", staticKeyword, type, m.getIdentifier(), parameters, body);
    }

    public static String prettyPrint(Field f) {
        return String.format("public %s %s;", prettyPrint(f.getType()), f.getIdentifier());
    }

    public static String prettyPrint(Class c) {
        // Methods and fields are sorted alphabetically
        List<Method> methods = sortedBy(c.getMethods(), Method::getIdentifier);
        List<Field> fields = sortedBy(c.getFields(), Field::getIdentifier);
        String printedMethods = indent(lines(prettyPrintAll(methods, AstPrinter::prettyPrint)));
        String printedFields = indent(lines(prettyPrintAll(fields, AstPrinter::prettyPrint)));
        return String.format(lines(
                "class %s {",
                "%s",
                "%s",
                "}"), c.getIdentifier(), printedMethods, printedFields);
    }

    public static String prettyPrint(Program p) {
        // Classes are sorted alphabetically
        List<Class> classes = sortedBy(p.getClasses(), Class::getIdentifier);
        return lines(prettyPrintAll(classes, AstPrinter::prettyPrint));
    }

}
