package compiler;

import compiler.ast.Class;
import compiler.ast.*;
import compiler.resolution.DefinedClass;
import compiler.resolution.DefinedMethod;
import compiler.resolution.Definitions;
import compiler.resolution.IntrinsicMethod;
import compiler.types.TyResult;

import javax.swing.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class DotWriter {
    private final PrintWriter out;
    private final AstData<TyResult> types;

    public static class Node {
        private final AstNode ast;
        private final List<String> attributes;

        public Node(AstNode ast, String name) {
            this.ast = ast;
            this.attributes = new ArrayList<>();
            this.attributes.add(name);
        }

        public void addAttribute(String attr) {
            this.attributes.add(attr);
        }

        String toDot(AstData<TyResult> types) {
            if (this.ast == null) {
                return "";
            }

            var ty = types.get(this.ast);
            ty.ifPresent(tyResult -> this.addAttribute(String.format("ty=%s", tyResult)));

            var color = this.ast.isError() ? "red" : "black";

            return String.format("%s", ast.getID()) +
                    "[label=\"" +
                    String.join("\n", this.attributes) +
                    "\"" +
                    String.format(", color=\"%s\"", color) +
                    ", ordering=\"out\"" +
                    "]\n";
        }
    }

    DotWriter(PrintWriter out, AstData<TyResult> types) {
        this.out = out;
        this.types = types;
        this.out.println("digraph {");
    }

    void addNode(AstNode ast, String label) {
        var n = new DotWriter.Node(ast, label);

        this.out.println(n.toDot(this.types));
    }

    void addNode(Node n) {
        this.out.println(n.toDot(this.types));
    }

    void addEdge(AstNode start, AstNode end) {
        this.addEdge(start, end, "");
    }

    void addDataEdge(AstNode start, AstNode end) {
        this.out.format("%s -> %s [color=blue, constraint=false, style=dotted];\n", start.getID(), end.getID());
    }

    void addEdge(AstNode start, AstNode end, String label) {
        if (start == null || end == null) {
            return;
        }

        this.out.format("%s -> %s [label=\"%s\"];\n", start.getID(), end.getID(), label);
    }

    void finish() {
        this.out.println("}");
        this.out.flush();
    }
}

public class DumpAst {

    private final DotWriter out;
    private final Definitions definitions;

    private DumpAst(PrintWriter out, Definitions definitions, AstData<TyResult> types) {
        this.out = new DotWriter(out, types);
        this.definitions = definitions;
    }

    public static void dump(PrintWriter out, AstNode ast, Definitions definitions, AstData<TyResult> types) {
        var dumper = new DumpAst(out, definitions, types);
        dumper.dumpAst(ast);
        dumper.out.finish();
    }

    public static void dump(PrintWriter out, AstNode ast, Definitions definitions) {
        var dumper = new DumpAst(out, definitions, new AstData<>());
        dumper.dumpAst(ast);
        dumper.out.finish();
    }

    private void dumpAst(AstNode ast) {
        switch (ast) {
            case Program prog -> {
                this.out.addNode(prog, "Program");

                for (Class klass : prog.getClasses()) {
                    this.out.addEdge(prog, klass);
                    this.dumpAst(klass);
                }
            }
            case Class klass -> {
                this.out.addNode(klass, String.format("Class '%s'", klass.getIdentifier()));

                for (Field f : klass.getFields()) {
                    this.out.addEdge(klass, f);
                    this.dumpAst(f);
                }

                for (Method m : klass.getMethods()) {
                    this.out.addEdge(klass, m);
                    this.dumpAst(m);
                }
            }
            case Method method -> {
                this.out.addNode(method, String.format("Method '%s'", method.getIdentifier()));

                this.out.addEdge(method, method.getReturnType(), "returnType");
                this.dumpAst(method.getReturnType());

                for (Parameter param : method.getParameters()) {
                    this.out.addEdge(method, param);
                    this.dumpAst(param);
                }

                this.out.addEdge(method, method.getBody(), "body");
                this.dumpAst(method.getBody());
            }
            case Field field -> {
                this.out.addNode(field, String.format("Field '%s'", field.getIdentifier()));

                this.out.addEdge(field, field.getType(), "type");
                this.dumpAst(field.getType());
            }
            case Parameter param -> {
                this.out.addNode(param, String.format("Param '%s'", param.getIdentifier()));

                this.out.addEdge(param, param.getType(), "type");
                this.dumpAst(param.getType());
            }
            case Type ty -> this.dumpType(ty);
            case Statement stmt -> this.dumpStatement(stmt);
            case Expression expr -> this.dumpExpr(expr);
            case Identifier i -> out.addNode(i, String.format("ident '%s'", i.getContent()));
        }
    }

    private void dumpStatement(Statement stmt) {
        switch (stmt) {
            case Block block -> {
                this.out.addNode(block, "Block");


                int childIdx = 0;
                for (var child : block.getStatements()) {
                    this.out.addEdge(block, child, "" + childIdx);
                    this.dumpAst(child);

                    childIdx += 1;
                }
            }
            case EmptyStatement empty -> this.out.addNode(empty, "Empty");
            case IfStatement ifStmt -> {
                this.out.addNode(ifStmt, "If-Else");

                this.out.addEdge(ifStmt, ifStmt.getCondition(), "condition");
                this.dumpAst(ifStmt.getCondition());

                this.out.addEdge(ifStmt, ifStmt.getThenBody(), "then");
                this.dumpAst(ifStmt.getThenBody());

                var elseBody = ifStmt.getElseBody();
                if (elseBody.isPresent()) {
                    this.out.addEdge(ifStmt, elseBody.get(), "else");
                    this.dumpAst(elseBody.get());
                }
            }
            case ExpressionStatement exprStmt -> {
                this.out.addNode(exprStmt, "ExpressionStatement");

                this.out.addEdge(exprStmt, exprStmt.getExpression());
                this.dumpAst(exprStmt.getExpression());
            }
            case WhileStatement whileStmt -> {
                this.out.addNode(whileStmt, "While");

                this.out.addEdge(whileStmt, whileStmt.getCondition(), "condition");
                this.dumpAst(whileStmt.getCondition());

                this.out.addEdge(whileStmt, whileStmt.getBody(), "body");
                this.dumpAst(whileStmt.getBody());
            }
            case ReturnStatement retStmt -> {
                this.out.addNode(retStmt, "Return");

                var expr = retStmt.getExpression();
                if (expr.isPresent()) {
                    this.out.addEdge(retStmt, expr.get());
                    this.dumpAst(expr.get());
                }
            }
            case LocalVariableDeclarationStatement declStmt -> {
                this.out.addNode(declStmt, String.format("Decl '%s'", declStmt.getIdentifier()));

                this.out.addEdge(declStmt, declStmt.getType(), "type");
                this.dumpAst(declStmt.getType());

                var init = declStmt.getInitializer();
                if (init.isPresent()) {
                    this.out.addEdge(declStmt, init.get(), "init");
                    this.dumpAst(init.get());
                }
            }
        }
    }

    private void dumpExpr(Expression expr) {
        switch (expr) {
            case AssignmentExpression assign -> {
                this.out.addNode(assign, "Assign");

                this.out.addEdge(assign, assign.getLvalue());
                this.dumpAst(assign.getLvalue());
                this.out.addEdge(assign, assign.getRvalue());
                this.dumpAst(assign.getRvalue());
            }
            case BinaryOpExpression binaryOp -> {
                var op = binaryOp.getOperator();

                this.out.addNode(binaryOp, "" + op);

                this.out.addEdge(binaryOp, binaryOp.getLhs());
                this.dumpAst(binaryOp.getLhs());

                this.out.addEdge(binaryOp, binaryOp.getRhs());
                this.dumpAst(binaryOp.getRhs());
            }
            case UnaryExpression unaryOp -> {
                var op = unaryOp.getOperator();

                this.out.addNode(unaryOp, "" + op);

                this.out.addEdge(unaryOp, unaryOp.getExpression());
                this.dumpAst(unaryOp.getExpression());
            }
            case MethodCallExpression methodCall -> {
                var node = new DotWriter.Node(methodCall, String.format("Call '%s'", methodCall.getIdentifier()));

                var maybeMethod = this.definitions.getMethod(methodCall);
                if (maybeMethod.isPresent()) {
                    var methodDef = maybeMethod.get();

                    switch (methodDef) {
                        case DefinedMethod dm -> this.out.addDataEdge(methodCall, dm.getAstMethod());
                        case IntrinsicMethod ignored -> node.addAttribute("intrinsic");
                    }
                }

                this.out.addNode(node);

                var target = methodCall.getTarget();
                if (target.isPresent()) {
                    this.out.addEdge(methodCall, target.get(), "target");
                    this.dumpAst(target.get());
                }

                for (var arg : methodCall.getArguments()) {
                    this.out.addEdge(methodCall, arg);
                    this.dumpAst(arg);
                }
            }
            case FieldAccessExpression fieldAccess -> {
                this.out.addNode(fieldAccess, String.format("FieldAccess '%s'", fieldAccess.getIdentifier()));

                var maybeField = this.definitions.getField(fieldAccess);
                maybeField.ifPresent(field -> this.out.addDataEdge(fieldAccess, field));

                var target = fieldAccess.getTarget();
                this.out.addEdge(fieldAccess, target, "target");
                this.dumpAst(target);
            }
            case ArrayAccessExpression arrayAccess -> {
                this.out.addNode(arrayAccess, "ArrayAccess");

                this.out.addEdge(arrayAccess, arrayAccess.getTarget(), "target");
                this.dumpAst(arrayAccess.getTarget());

                this.out.addEdge(arrayAccess, arrayAccess.getIndexExpression());
                this.dumpAst(arrayAccess.getIndexExpression());
            }
            case NewArrayExpression newArray -> {
                this.out.addNode(newArray, String.format("New Array [%s]", newArray.getDimensions()));

                this.out.addEdge(newArray, newArray.getType(), "type");
                this.dumpType(newArray.getType());

                this.out.addEdge(newArray, newArray.getFirstDimensionSize(), "dim");
                this.dumpAst(newArray.getFirstDimensionSize());
            }
            case Reference ref -> {
                var def = this.definitions.getReference(ref);
                def.ifPresent(variableDefinition -> this.out.addDataEdge(ref, (AstNode) variableDefinition));
                this.out.addNode(ref, String.format("Ref '%s'", ref.getIdentifier()));
            }
            case NullExpression nullExpr -> this.out.addNode(nullExpr, "Null");
            case BoolLiteral boolLit -> this.out.addNode(boolLit, String.format("Bool '%s'", boolLit.getValue()));
            case IntLiteral intLit -> this.out.addNode(intLit, String.format("Int '%s'", intLit.getValue()));
            case ThisExpression thisExpr -> this.out.addNode(thisExpr, "This");
            case NewObjectExpression newObject -> {
                this.out.addNode(newObject, "New Object");

                this.out.addEdge(newObject, newObject.getType(), "type");
                this.dumpAst(newObject.getType());
            }
        }
    }

    private void dumpType(Type type) {
        switch (type) {
            case ArrayType arrayTy -> {
                this.out.addNode(arrayTy, "Array");
                this.out.addEdge(arrayTy, arrayTy.getChildType());
                this.dumpAst(arrayTy.getChildType());
            }
            case IntType intTy -> this.out.addNode(intTy, "int");
            case BoolType boolTy -> this.out.addNode(boolTy, "boolean");
            case VoidType voidTy -> this.out.addNode(voidTy, "void");
            case ClassType classTy -> {
                var maybeClassDef = this.definitions.getClass(classTy);
                var node = new DotWriter.Node(classTy, String.format("class '%s'", classTy.getIdentifier()));
                if (maybeClassDef.isPresent()) {
                    var classDef = maybeClassDef.get();
                    if (classDef instanceof DefinedClass definedClassDef) {
                        this.out.addDataEdge(classTy, definedClassDef.getAstClass());
                    } else {
                        node.addAttribute("intrinsic");
                    }
                }
                this.out.addNode(node);
            }
        }
    }
}
