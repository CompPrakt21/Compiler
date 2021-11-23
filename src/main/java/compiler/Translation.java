package compiler;

import compiler.ast.Program;
import compiler.ast.*;
import compiler.semantic.AstData;
import compiler.semantic.ConstantFolding;
import compiler.semantic.SparseAstData;
import compiler.semantic.WellFormed;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.semantic.resolution.NameResolution;
import compiler.types.*;
import firm.Type;
import firm.*;
import firm.bindings.binding_ircons;
import firm.nodes.Block;
import firm.nodes.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("DuplicateBranchesInSwitch")
public class Translation {

    private final NameResolution.NameResolutionResult resolution;
    private final AstData<Integer> constants;
    private final AstData<Integer> localsVarsInMethod;

    private final Map<Ty, Type> firmTypes;
    private final AstData<Integer> variableId; // maps variable definitions to their firm variable ids.
    private final List<Node> returns;
    private final CompoundType globalType;
    private int thisVariableId; // firm variable id for implicit this parameter
    private int nextVariableId;
    private Construction construction;

    public Translation(NameResolution.NameResolutionResult resolution, ConstantFolding.ConstantFoldingResult constants, WellFormed.WellFormedResult wellFormed) {
        this.resolution = resolution;
        this.constants = constants.constants();
        this.localsVarsInMethod = wellFormed.variableCounts();

        Firm.init();
        this.firmTypes = new HashMap<>();
        this.variableId = new SparseAstData<>();
        this.returns = new ArrayList<>();
        this.globalType = firm.Program.getGlobalType();

        this.nextVariableId = 0;
    }

    private int newVariableId() {
        var varId = this.nextVariableId;
        this.nextVariableId += 1;
        return varId;
    }

    private MethodType getMethodType(MethodDefinition method) {
        var paramTypes = Stream.concat(
                Stream.of(this.firmTypes.get(method.getContainingClass().orElseThrow())),
                method.getParameterTy().stream().map(ty -> getFirmType((Ty) ty))
        ).toArray(Type[]::new);

        Type[] returnTypes;
        var returnType = method.getReturnTy();
        returnTypes = returnType instanceof VoidTy ? new Type[0] : new Type[]{getFirmType((Ty) returnType)};
        return new MethodType(paramTypes, returnTypes);
    }

    private Type getFirmType(Ty type) {
        var result = switch (type) {
            case BoolTy ignored -> {
                if (this.firmTypes.containsKey(type)) {
                    yield this.firmTypes.get(type);
                } else {
                    var firmType = new PrimitiveType(Mode.getBu());
                    this.firmTypes.put(type, firmType);
                    yield firmType;
                }
            }
            case IntTy ignored -> {
                if (this.firmTypes.containsKey(type)) {
                    yield this.firmTypes.get(type);
                } else {
                    var firmType = new PrimitiveType(Mode.getIs());
                    this.firmTypes.put(type, firmType);
                    yield firmType;
                }
            }
            case ArrayTy arrTy -> {
                if (this.firmTypes.containsKey(arrTy)) {
                    yield this.firmTypes.get(arrTy);
                } else {
                    var elemType = getFirmType(arrTy.getChildTy());
                    var firmType = new PointerType(elemType);

                    this.firmTypes.put(arrTy, firmType);
                    yield firmType;
                }
            }
            case ClassTy clsTy -> {
                if (this.firmTypes.containsKey(clsTy)) {
                    yield this.firmTypes.get(clsTy);
                } else {
                    var firmType = new StructType(clsTy.getName());
                    var clsPtr = new PointerType(firmType);
                    this.firmTypes.put(clsTy, clsPtr);
                    yield clsPtr;
                }
            }
            case NullTy ignored -> throw new UnsupportedOperationException("void");
        };
        return result;
    }

    private Node translateLiteral(AstNode literal) {
        var res = switch (literal) {
            case BoolLiteral lit -> construction.newConst(lit.getValue() ? 1 : 0, Mode.getBu());
            case IntLiteral lit -> {
                var value = this.constants.get(lit).orElseThrow();
                yield construction.newConst(value, Mode.getIs());
            }
            default -> throw new AssertionError("translateLiteral called with " + literal);
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
            case Multiplication -> construction.newMul(lhs, rhs);
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
                        construction.setVariable(variableId.get(var).orElseThrow(), rhs);
                        yield rhs;
                    }
                    case FieldAccessExpression fieldAccess -> {
                        throw new UnsupportedOperationException();
                    }
                    case ArrayAccessExpression arrayAccess -> {
                        throw new UnsupportedOperationException();
                    }
                    default -> throw new AssertionError("Not an lvalue"); //TODO
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
                var definition = this.resolution.definitions().getReference(expr).orElseThrow();

                if (definition instanceof LocalVariableDeclarationStatement || definition instanceof Parameter) {
                    yield construction.getVariable(variableId.get((AstNode) definition).orElseThrow(), Mode.getIs());
                } else {
                    assert definition instanceof Field;
                    throw new UnsupportedOperationException();
                }
            }
            case BoolLiteral expr -> translateLiteral(expr);
            case IntLiteral expr -> translateLiteral(expr);
        };
    }

    private void translateStatement(Statement statement) {
        switch (statement) {
            case EmptyStatement ignored -> {}
            case ExpressionStatement stmt -> translateExpr(stmt.getExpression());
            case IfStatement stmt -> throw new UnsupportedOperationException();
            case LocalVariableDeclarationStatement stmt -> {
                var statementId = this.newVariableId();
                variableId.set(stmt, statementId);
                if (stmt.getInitializer().isPresent()) {
                    var node = translateExpr(stmt.getInitializer().get());
                    construction.setVariable(statementId, node);
                }
            }
            case ReturnStatement stmt -> {
                Node[] rhs = stmt.getExpression().map(expr -> new Node[]{translateExpr(expr)}).orElse(new Node[0]);
                var ret = construction.newReturn(construction.getCurrentMem(), rhs);
                this.returns.add(ret);
            }
            case WhileStatement stmt -> throw new UnsupportedOperationException();
            case compiler.ast.Block block -> throw new UnsupportedOperationException();
        }
    }

    private Graph genGraphForMethod(DefinedMethod methodDef) {
        var name = methodDef.getName();
        var type = getMethodType(methodDef);

        Entity methodEnt = new Entity(globalType, name, type);
        if ("main".equals(name)) {
            Graph graph = new Graph(methodEnt, 10);
            return graph;
        }

        var numberLocalVars = this.localsVarsInMethod.get(methodDef.getAstMethod()).orElseThrow();
        var numberParameters = methodDef.getParameterTy().size();
        var numberFirmVars = numberLocalVars + numberParameters + 1; // +1 is implicit this argument
        Graph graph = new Graph(methodEnt, numberFirmVars);

        construction = new Construction(graph);

        Node startNode = construction.newStart();
        Node memProj = construction.newProj(startNode, Mode.getM(), 0);
        construction.setCurrentMem(memProj);
        Node argsProj = construction.newProj(startNode, Mode.getT(), 2);

        Node thisArg = construction.newProj(argsProj, Mode.getP(), 0);
        this.thisVariableId = this.newVariableId();
        construction.setVariable(this.thisVariableId, thisArg);

        Method method = methodDef.getAstMethod();
        Node arg;
        int index = 1;
        for (var param : method.getParameters()) {
            var ty = (Ty) this.resolution.bindingTypes().get(param).orElseThrow();

            Mode mode = switch (ty) {
                case IntTy ignored -> Mode.getIs();
                case BoolTy ignored -> Mode.getBu();
                case ClassTy ignored -> Mode.getP();
                case ArrayTy ignored -> Mode.getP();
                case NullTy ignored -> throw new AssertionError("Invalid parameter type");
            };

            arg = construction.newProj(argsProj, mode, index);
            var paramVariableId = this.newVariableId();
            variableId.set(param, paramVariableId);
            construction.setVariable(paramVariableId, arg);
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

    public void translate(Program ast) {
        // TODO: Fill classTypes

        for (var classTy : this.resolution.classes()) {
            CompoundType classType = (CompoundType) ((PointerType) getFirmType(classTy)).getPointsTo();

            for (var methodDef : classTy.getMethods().values()) {
                // We don't generate code for intrinsic methods.
                if (methodDef instanceof DefinedMethod definedMethod) {
                    Graph graph = genGraphForMethod(definedMethod);
                    Dump.dumpGraph(graph, methodDef.getName());
                }
            }
            for (var field : classTy.getFields().values()) {
                Type fieldType = getFirmType((Ty) this.resolution.bindingTypes().get(field).orElseThrow());
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
