package compiler;

import compiler.ast.*;
import compiler.ast.Program;
import compiler.semantic.resolution.DefinedClass;
import compiler.semantic.resolution.IntrinsicClass;
import compiler.semantic.resolution.MethodDefinition;
import compiler.semantic.resolution.NameResolution;
import compiler.types.*;
import firm.*;
import firm.Type;
import firm.bindings.binding_ircons;
import firm.nodes.Block;
import firm.nodes.Node;
import firm.nodes.Pin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Translation {

    private final Map<Ty, Type> primitiveTypes;
    private final Map<String, PointerType> classTypes;
    private final CompoundType globalType;
    private Construction construction;

    public Translation() {
        Firm.init();
        this.primitiveTypes = new HashMap<>();
        this.classTypes = new HashMap<>();
        this.globalType = firm.Program.getGlobalType();
    }

    private Type getBoolType() {
        return new PrimitiveType(Mode.getBu());
    }

    private Type getIntType() {
        return new PrimitiveType(Mode.getIs());
    }

    private Type getMethodType(MethodDefinition method) {
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
                Node proj = construction.newProj(div, Mode.getM(), 0);
                construction.setCurrentMem(proj);
                yield div;
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
            case AssignmentExpression expr -> throw new UnsupportedOperationException();
            case ArrayAccessExpression expr -> throw new UnsupportedOperationException();
            case MethodCallExpression expr -> throw new UnsupportedOperationException();
            case NewArrayExpression expr -> throw new UnsupportedOperationException();
            case NewObjectExpression expr -> throw new UnsupportedOperationException();
            case NullExpression expr -> throw new UnsupportedOperationException("null");
            case ThisExpression expr -> throw new UnsupportedOperationException();
            case UnaryExpression expr -> translateUnaryOp(expr);
            case Reference expr -> throw new UnsupportedOperationException();
            case BoolLiteral expr -> translateLiteral(expr);
            case IntLiteral expr -> translateLiteral(expr);
        };
    }

    private Graph genGraphForMethod(MethodDefinition method) {
        var name = method.getName();
        var type = getMethodType(method);

        Entity methodEnt = new Entity(globalType, name, type);
        if (!"init".equals(name)) {
            Graph graph = new Graph(methodEnt, 10);
            return graph;
        }

        Graph graph = new Graph(methodEnt, 10);

        construction = new Construction(graph);

        Node startNode = construction.newStart();
        Node memProj = construction.newProj(startNode, Mode.getM(), 0);
        Node argsProj = construction.newProj(startNode, Mode.getT(), 2);

        Node thisArg = construction.newProj(argsProj, Mode.getP(), 0);
        Node firstArg = construction.newProj(argsProj, Mode.getIs(), 1);
        Node secondArg = construction.newProj(argsProj, Mode.getIs(), 2);

        Node add = construction.newAdd(firstArg, secondArg);
        //graph.keepAlive(add);

        //Node constNode = construction.newConst(0, Mode.getIs());
        Node returnNode = construction.newReturn(memProj, new Node[]{add});
        Block endBlock = construction.getGraph().getEndBlock();
        endBlock.addPred(returnNode);

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
