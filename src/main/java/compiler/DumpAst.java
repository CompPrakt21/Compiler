package compiler;

import compiler.ast.Class;
import compiler.ast.*;
import compiler.types.TyResult;

import java.io.PrintWriter;

class DotWriter {
    private final PrintWriter out;
    private final AstData<TyResult> types;

    DotWriter(PrintWriter out, AstData<TyResult> types) {
        this.out = out;
        this.types = types;
        this.out.println("digraph {");
    }

    void addNode(AstNode ast, String label) {
        if (ast == null) {
            return;
        }

        String color = ast.isError() ? "red" : "black";

        String dotLabel = String.format("%s", label);

        var type = this.types.get(ast);
        if (type.isPresent()) {
            dotLabel += String.format("\n%s", type.get());
        }

        this.out.format("%s [label=\"%s\", color=%s, ordering=\"out\"];\n", ast.getID(), dotLabel, color);
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
    private final AstData<AstNode> definitions;

    private DumpAst(PrintWriter out, AstData<AstNode> definitions, AstData<TyResult> types) {
        this.out = new DotWriter(out, types);
        this.definitions = definitions;
    }

    public static void dump(PrintWriter out, AstNode ast, AstData<AstNode> definitions, AstData<TyResult> types) {
        var dumper = new DumpAst(out, definitions, types);
        dumper.dumpAst(ast);
        dumper.out.finish();
    }

    public static void dump(PrintWriter out, AstNode ast, AstData<AstNode> definitions) {
        var dumper = new DumpAst(out, definitions, new AstData<>());
        dumper.dumpAst(ast);
        dumper.out.finish();
    }

    private void dumpAst(AstNode ast) {
        var dataEdge = definitions.get(ast);

        dataEdge.ifPresent(astNode -> this.out.addDataEdge(ast, astNode));

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
                this.out.addNode(methodCall, String.format("Call '%s'", methodCall.getIdentifier()));

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

                this.out.addEdge(newArray, newArray.getFirstDimensionSize(), "dim");
                this.dumpAst(newArray.getFirstDimensionSize());
            }
            case Reference ref -> this.out.addNode(ref, String.format("Ref '%s'", ref.getIdentifier()));
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
            case ClassType classTy -> this.out.addNode(classTy, String.format("class '%s'", classTy.getIdentifier()));
        }
    }
}
