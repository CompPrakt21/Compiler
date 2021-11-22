package compiler;

import compiler.ast.*;
import compiler.ast.Program;
import compiler.semantic.resolution.*;
import compiler.types.*;
import firm.*;
import firm.Type;
import firm.bindings.binding_ircons;
import firm.nodes.Block;
import firm.nodes.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Translation {

    private final Map<Ty, Type> primitiveTypes;
    private final Map<String, PointerType> classTypes;
    private final Map<String, Integer> variableId;
    private final List<Node> returns;
    private final CompoundType globalType;
    private int nextVariableId;
    private Construction construction;

    public Translation() {
        Firm.init();
        this.primitiveTypes = new HashMap<>();
        this.classTypes = new HashMap<>();
        this.variableId = new HashMap<>();
        this.returns = new ArrayList<>();
        this.globalType = firm.Program.getGlobalType();
        this.nextVariableId = 0;
    }

    private Type getBoolType() {
        return new PrimitiveType(Mode.getBu());
    }

    private Type getIntType() {
        return new PrimitiveType(Mode.getIs());
    }

    private MethodType getMethodType(MethodDefinition method) {
        var paramTypes = Stream.concat(
                Stream.of(classTypes.get(method.getContainingClass().get().getName())),
                method.getParameterTy().stream().map(ty -> getFirmType((Ty) ty))
        ).toArray(Type[]::new);

        Type[] returnTypes;
        var returnType = method.getReturnTy();
        returnTypes = switch (returnType) {
            case VoidTy ignored -> new Type[0];
            default -> new Type[]{getFirmType((Ty) returnType)};
        };
        return new MethodType(paramTypes, returnTypes);
    }

    private CompoundType getClassType(String identifier) {
        return new StructType(identifier);
    }

    private Type getArrayType(ArrayTy arrTy) {
        var elemType = getFirmType(arrTy.getChildTy());
        return new firm.PointerType(elemType);
    }

    private Type getFirmType(Ty type) {
        // TODO: Should ArrayTypes also be cached?
        var result = switch (type) {
            case BoolTy ignored -> {
                if (primitiveTypes.containsKey(type)) {
                    yield primitiveTypes.get(type);
                }
                var firmType = getBoolType();
                primitiveTypes.put(type, firmType);
                yield firmType;
            }
            case IntTy ignored -> {
                if (primitiveTypes.containsKey(type)) {
                    yield primitiveTypes.get(type);
                }
                var firmType = getIntType();
                primitiveTypes.put(type, firmType);
                yield firmType;
            }
            case ArrayTy arrTy -> getArrayType(arrTy);
            case ClassTy clsTy -> {
                if (classTypes.containsKey(clsTy.toString())) {
                    yield classTypes.get(clsTy.toString());
                }
                var definition = clsTy.getDefinition();
                var firmType = switch (definition) {
                    case DefinedClass ignored -> getClassType(clsTy.toString());
                    case IntrinsicClass ignored -> getClassType("String");
                };
                var clsPtr = new PointerType(firmType);
                classTypes.put(clsTy.toString(), clsPtr);
                yield clsPtr;
            }
            case NullTy ignored -> throw new UnsupportedOperationException("void");
        };
        return result;
    }

    private Node translateLiteral(AstNode literal) {
        var res = switch (literal) {
            case BoolLiteral lit -> construction.newConst(lit.getValue() ? 1 : 0, Mode.getBu());
            // TODO: Why is IntLiteral::getValue() a String?
            case IntLiteral lit -> construction.newConst(Integer.parseInt(lit.getValue()), Mode.getIs());
            default -> throw new UnsupportedOperationException("translateLiteral called with " + literal);
        };
        return res;
    }

    private Node translateBinOp(BinaryOpExpression expr) {
        Node lhs = translateExpr(expr.getLhs());
        Node rhs = translateExpr(expr.getRhs());
        var ret = switch (expr.getOperator()) {
            case And -> construction.newAnd(lhs, rhs);
            case Or -> construction.newOr(lhs, rhs);
            case Equal -> construction.newCmp(lhs, rhs, Relation.Equal);
            case NotEqual -> throw new UnsupportedOperationException("NEQ");
            case Less -> construction.newCmp(lhs, rhs, Relation.Less);
            case LessEqual -> construction.newCmp(lhs, rhs, Relation.LessEqual);
            case Greater -> construction.newCmp(lhs, rhs, Relation.Greater);
            case GreaterEqual -> construction.newCmp(lhs, rhs, Relation.GreaterEqual);
            case Addition -> construction.newAdd(lhs, rhs);
            case Subtraction -> construction.newSub(lhs, rhs);
            case Multiplication -> construction.newMul(lhs, rhs); //TODO: Mul or Mulh?
            case Division -> {
                Node div = construction.newDiv(construction.getCurrentMem(), lhs, rhs, binding_ircons.op_pin_state.op_pin_state_pinned);
                Node memProj = construction.newProj(div, Mode.getM(), 0);
                Node resProj = construction.newProj(div, Mode.getIs(), 1);
                construction.setCurrentMem(memProj);
                yield resProj;
            }
            case Modulo -> {
                Node div = construction.newMod(construction.getCurrentMem(), lhs, rhs, binding_ircons.op_pin_state.op_pin_state_pinned);
                Node proj = construction.newProj(div, Mode.getM(), 0);
                construction.setCurrentMem(proj);
                yield div;
            }
        };
        return ret;
    }

    private Node translateUnaryOp(UnaryExpression expr) {
        Node rhs = translateExpr(expr.getExpression());
        return switch (expr.getOperator()) {
            case LogicalNot -> construction.newNot(rhs);
            case Negate -> construction.newMinus(rhs);
        };
    }

    private Node translateExpr(Expression root) {
        return switch (root) {
            case BinaryOpExpression expr -> translateBinOp(expr);
            case FieldAccessExpression expr -> throw new UnsupportedOperationException();
            case AssignmentExpression expr -> {
                switch (expr.getLvalue()) {
                    case Reference var -> {
                        // TODO: What if not a local var
                        var rhs = translateExpr(expr.getRvalue());
                        construction.setVariable(variableId.get(var.getIdentifier().toString()), rhs);
                        yield rhs;
                    }
                    default -> throw new UnsupportedOperationException(); //TODO
                }
            }
            case ArrayAccessExpression expr -> throw new UnsupportedOperationException();
            case MethodCallExpression expr -> throw new UnsupportedOperationException();
            case NewArrayExpression expr -> throw new UnsupportedOperationException();
            case NewObjectExpression expr -> throw new UnsupportedOperationException();
            case NullExpression expr -> throw new UnsupportedOperationException("null");
            case ThisExpression expr -> throw new UnsupportedOperationException();
            case UnaryExpression expr -> translateUnaryOp(expr);
            case Reference expr -> {
                // TODO: What if not a local var
                // TODO: different mode
                yield construction.getVariable(variableId.get(expr.getIdentifier().toString()), Mode.getIs());
            }
            case BoolLiteral expr -> translateLiteral(expr);
            case IntLiteral expr -> translateLiteral(expr);
        };
    }

    private Node translateStatement(Statement statement) {
        var res = switch (statement) {
            case EmptyStatement stmt -> construction.newBad(Mode.getIs()); // TODO: stmts
            case ExpressionStatement stmt -> translateExpr(stmt.getExpression());
            case IfStatement stmt -> throw new UnsupportedOperationException();
            case LocalVariableDeclarationStatement stmt -> {
                variableId.put(stmt.getIdentifier().toString(), nextVariableId);
                nextVariableId++;
                yield null;
            }
            case ReturnStatement stmt -> {
                Node[] rhs = stmt.getExpression().map(expr -> new Node[]{translateExpr(expr)}).orElse(new Node[0]);
                var ret =  construction.newReturn(construction.getCurrentMem(), rhs);
                returns.add(ret);
                yield ret;
            }
            case WhileStatement stmt -> throw new UnsupportedOperationException();
            case compiler.ast.Block block -> throw new UnsupportedOperationException();
        };
        return res;
    }

    private Graph genGraphForMethod(MethodDefinition methodDef) {
        var name = methodDef.getName();
        var type = getMethodType(methodDef);

        Entity methodEnt = new Entity(globalType, name, type);
        if ("main".equals(name)) {
            Graph graph = new Graph(methodEnt, 10);
            return graph;
        }

        Graph graph = new Graph(methodEnt, 10);

        construction = new Construction(graph);

        Node startNode = construction.newStart();
        Node memProj = construction.newProj(startNode, Mode.getM(), 0);
        construction.setCurrentMem(memProj);
        Node argsProj = construction.newProj(startNode, Mode.getT(), 2);

        Node thisArg = construction.newProj(argsProj, Mode.getP(), 0);
        construction.setVariable(nextVariableId, thisArg);
        variableId.put("this", nextVariableId);
        nextVariableId++;

        Method method = ((DefinedMethod)methodDef).getAstMethod();
        Node arg;
        int index = 1;
        for (var param : method.getParameters()) {
            Mode mode = switch (param.getType()) {
                case IntType ignored -> Mode.getIs();
                case BoolType ignored -> Mode.getBu();
                default -> throw new UnsupportedOperationException(); // TODO: Other tys
            };
            arg = construction.newProj(argsProj, mode, index);
            construction.setVariable(nextVariableId, arg);
            variableId.put(param.getIdentifier().toString(), nextVariableId);
            nextVariableId++;
            index++;
        }


        var body = method.getBody();

        for (var statement : body.getStatements()) {
            translateStatement(statement);
        }

        Block endBlock = construction.getGraph().getEndBlock();
        for (var ret : returns) {
            endBlock.addPred(ret);
        }

        construction.finish();

        return graph;
    }

    public void translate(Program ast, NameResolution.NameResolutionResult nres) {
        // TODO: Fill classTypes

        for (var classTy : nres.classes()) {
            CompoundType classType = (CompoundType) ((PointerType) getFirmType(classTy)).getPointsTo();

            for (var methodDef : classTy.getDefinition().getMethods().values()) {
                Graph graph = genGraphForMethod(methodDef);
                Dump.dumpGraph(graph, methodDef.getName());
            }
            for (var field : classTy.getDefinition().getFields().values()) {
                Type fieldType = getFirmType((Ty) nres.bindingTypes().get(field).get());
                Entity fieldEnt = new Entity(classType, field.getIdentifier().toString(), fieldType);
            }
        }

        try {
            Dump.dumpTypeGraph("types.vcg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
