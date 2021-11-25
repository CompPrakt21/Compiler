package compiler;

import compiler.ast.Program;
import compiler.ast.*;
import compiler.semantic.AstData;
import compiler.semantic.ConstantFolding;
import compiler.semantic.SparseAstData;
import compiler.semantic.WellFormed;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.semantic.resolution.NameResolution;
import compiler.types.*;
import firm.Type;
import firm.*;
import firm.bindings.binding_ircons;
import firm.nodes.Block;
import firm.nodes.Node;

import javax.swing.plaf.ComponentUI;
import java.io.IOException;
import java.sql.Ref;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("DuplicateBranchesInSwitch")
public class Translation {

    private final NameResolution.NameResolutionResult resolution;
    private final AstData<Integer> constants;
    private final AstData<Integer> localsVarsInMethod;

    private final Map<Ty, Type> firmTypes;
    private final AstData<Integer> variableId; // maps variable definitions to their firm variable ids.
    private final AstData<Entity> entities; // maps method and fields to their respective entity.
    private final Map<IntrinsicMethod, Entity> intrinsicEntities;
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
        this.entities = new SparseAstData<>();
        this.intrinsicEntities = new HashMap<>();
        this.returns = new ArrayList<>();
        this.globalType = firm.Program.getGlobalType();

        this.nextVariableId = 0;
    }

    private int newVariableId() {
        var varId = this.nextVariableId;
        this.nextVariableId += 1;
        return varId;
    }

    private MethodType getMethodType(DefinedMethod method) {
        var paramTypes = Stream.concat(
                Stream.of(getFirmType(method.getContainingClass().orElseThrow())),
                method.getParameterTy().stream().map(ty -> getFirmType((Ty) ty))
        ).toArray(Type[]::new);

        Type[] returnTypes;
        var returnType = method.getReturnTy();
        returnTypes = returnType instanceof VoidTy ? new Type[0] : new Type[]{getFirmType((Ty) returnType)};
        return new MethodType(paramTypes, returnTypes);
    }

    private MethodType getIntrinsicMethodType(IntrinsicMethod method) {
        var paramTypes = method.getParameterTy().stream().map(ty -> getFirmType((Ty) ty)).toArray(Type[]::new);
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
            case NullTy nullTy -> {
                if (this.firmTypes.containsKey(nullTy)) {
                    yield this.firmTypes.get(nullTy);
                } else {
                    throw new AssertionError("This should never be needed.");
                }
            }
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
            case NotEqual -> {
                var equalNode = construction.newCmp(lhs, rhs, Relation.Equal);
                yield construction.newNot(equalNode);
            }
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

    private Node translateFieldExprToLValue(Node targetNode, Expression expr) {
        assert targetNode.getMode().equals(Mode.getP());

        Field field;
        if (expr instanceof FieldAccessExpression fieldAccessExpression) {
            field = this.resolution.definitions().getField(fieldAccessExpression).orElseThrow();
        } else if (expr instanceof Reference ref) {
            field = (Field) this.resolution.definitions().getReference(ref).orElseThrow();
        } else {
            throw new AssertionError("Unrecheable");
        }

        var memberEntity = this.entities.get(field).orElseThrow();
        var member = construction.newMember(targetNode, memberEntity);

        return member;
    }

    private Node translateFieldAccessExpr(Node targetNode, Expression expr) {
        var fieldPtr = translateFieldExprToLValue(targetNode, expr);

        var mem = construction.getCurrentMem();
        var exprTy = (Ty)this.resolution.expressionTypes().get(expr).orElseThrow();
        var exprFirmType = getFirmType(exprTy);

        var load = construction.newLoad(mem, fieldPtr, exprFirmType.getMode());

        var memProj = construction.newProj(load, Mode.getM(), 0);
        construction.setCurrentMem(memProj);

        return construction.newProj(load, exprFirmType.getMode(), 1);
    }

    private void translateFieldAssignment(Node target, Expression lValue, Node rValue) {
        var fieldPtr = translateFieldExprToLValue(target, lValue);

        var mem = construction.getCurrentMem();

        var store = construction.newStore(mem, fieldPtr, rValue);
        var memProj = construction.newProj(store, Mode.getM(), 0);

        construction.setCurrentMem(memProj);
    }

    private Optional<Node> translateMethodCallExpression(MethodCallExpression expr) {
        var methodDef = this.resolution.definitions().getMethod(expr).orElseThrow();

        var methodEntity = switch (methodDef) {
            case DefinedMethod definedMethod -> this.entities.get(definedMethod.getAstMethod()).orElseThrow();
            case IntrinsicMethod intrinsicMethod -> this.intrinsicEntities.get(intrinsicMethod);
        };

        Optional<Node> targetNode = switch (methodDef) {
            case DefinedMethod ignored -> Optional.of(
                    expr.getTarget()
                        .map(this::translateExpr)
                        .orElseGet(() -> construction.getVariable(this.thisVariableId, Mode.getP()))
            );
            case IntrinsicMethod ignored -> Optional.empty();
        };

        var addrNode = construction.newAddress(methodEntity);
        var methodType = methodEntity.getType();

        var arguments = Stream.concat(
                targetNode.stream(),
                expr.getArguments().stream().map(this::translateExpr)
        ).toArray(Node[]::new);

        var mem = construction.getCurrentMem();

        var callNode = construction.newCall(mem, addrNode, arguments, methodType);

        var memProj = construction.newProj(callNode, Mode.getM(), 0);
        construction.setCurrentMem(memProj);

        var returnTy = methodDef.getReturnTy();
        if (returnTy instanceof VoidTy) {
            return Optional.empty();
        } else {
            assert returnTy instanceof Ty;
            var returnValuesProj = construction.newProj(callNode, Mode.getT(), 1);

            var firmReturnType = getFirmType((Ty)returnTy);

            var returnValueProj = construction.newProj(returnValuesProj, firmReturnType.getMode(), 0);

            return Optional.of(returnValueProj);
        }
    }

    private Node translateExpr(Expression root) {
        return switch (root) {
            case BinaryOpExpression expr -> translateBinOp(expr);
            case FieldAccessExpression expr -> {
                var targetNode = translateExpr(expr.getTarget());
                yield translateFieldAccessExpr(targetNode, expr);
            }
            case AssignmentExpression expr -> {
                var rhs = translateExpr(expr.getRvalue());

                switch (expr.getLvalue()) {
                    case Reference var -> {
                        var definition = this.resolution.definitions().getReference(var).orElseThrow();

                        if (definition instanceof LocalVariableDeclarationStatement || definition instanceof Parameter) {
                            var firmVarId = variableId.get((AstNode) definition).orElseThrow();
                            construction.setVariable(firmVarId, rhs);
                        } else {
                            assert definition instanceof Field;
                            var thisNode = construction.getVariable(this.thisVariableId, Mode.getP());
                            translateFieldAssignment(thisNode, var, rhs);
                        }
                    }
                    case FieldAccessExpression fieldAccess -> {
                        var targetNode = translateExpr(fieldAccess.getTarget());
                        translateFieldAssignment(targetNode, fieldAccess, rhs);
                    }
                    case ArrayAccessExpression arrayAccess -> {
                        throw new UnsupportedOperationException();
                    }
                    default -> throw new AssertionError("Not an lvalue");
                }

                yield rhs;
            }
            case ArrayAccessExpression expr -> throw new UnsupportedOperationException();
            case MethodCallExpression expr -> {
                var definition = this.resolution.definitions().getMethod(expr).orElseThrow();

                var node = translateMethodCallExpression(expr);

                yield node.orElseThrow(() -> new AssertionError("MethodCallExpression of void methods can only be directly after ExpressionStatements."));
            }
            case NewArrayExpression expr -> throw new UnsupportedOperationException();
            case NewObjectExpression expr -> throw new UnsupportedOperationException();
            case NullExpression expr -> throw new UnsupportedOperationException("null");
            case ThisExpression expr -> construction.getVariable(this.thisVariableId, Mode.getP());
            case UnaryExpression expr -> translateUnaryOp(expr);
            case Reference expr -> {
                var definition = this.resolution.definitions().getReference(expr).orElseThrow();

                if (definition instanceof LocalVariableDeclarationStatement || definition instanceof Parameter) {
                    var defType = (Ty) this.resolution.expressionTypes().get(expr).orElseThrow();
                    var firmType = this.getFirmType(defType);
                    var mode = firmType.getMode();

                    yield construction.getVariable(variableId.get((AstNode) definition).orElseThrow(), mode);
                } else {
                    assert definition instanceof Field;
                    var thisNode = construction.getVariable(this.thisVariableId, Mode.getP());
                    yield translateFieldAccessExpr(thisNode, expr);
                }
            }
            case BoolLiteral expr -> translateLiteral(expr);
            case IntLiteral expr -> translateLiteral(expr);
        };
    }

    private void translateStatement(Statement statement) {
        switch (statement) {
            case EmptyStatement ignored -> {}
            case ExpressionStatement stmt -> {
                if (stmt.getExpression() instanceof MethodCallExpression methodCall) {
                    translateMethodCallExpression(methodCall);
                } else {
                    translateExpr(stmt.getExpression());
                }
            }
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
        this.nextVariableId = 0;
        this.returns.clear();
        this.thisVariableId = -1;

        var name = methodDef.getName();
        var methodEnt = this.entities.get(methodDef.getAstMethod()).orElseThrow();

        var isMainMethod = name.equals("main");

        var numberLocalVars = this.localsVarsInMethod.get(methodDef.getAstMethod()).orElseThrow();
        var numberParameters = methodDef.getParameterTy().size();
        var numberFirmVars = numberLocalVars + numberParameters + (isMainMethod ? 0 : 1); // +1 is implicit this argument
        Graph graph = new Graph(methodEnt, numberFirmVars);

        construction = new Construction(graph);

        Node startNode = construction.newStart();
        Node memProj = construction.newProj(startNode, Mode.getM(), 0);
        construction.setCurrentMem(memProj);
        Node argsProj = construction.newProj(startNode, Mode.getT(), 2);

        if (!isMainMethod) {
            Node thisArg = construction.newProj(argsProj, Mode.getP(), 0);
            this.thisVariableId = this.newVariableId();
            construction.setVariable(this.thisVariableId, thisArg);
        }

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

        Statement lastStatement = null;
        for (var statement : body.getStatements()) {
            lastStatement = statement;
            translateStatement(statement);
        }

        if (!(lastStatement instanceof ReturnStatement)) {
            assert methodDef.getReturnTy() instanceof VoidTy;
            var mem = construction.getCurrentMem();
            returns.add(construction.newReturn(mem, new Node[]{}));
        }

        Block endBlock = construction.getGraph().getEndBlock();
        for (var ret : returns) {
            endBlock.addPred(ret);
        }

        construction.finish();

        return graph;
    }

    public void translate(Program ast) {
        for (var classTy : this.resolution.classes()) {
            CompoundType classType = (CompoundType) ((PointerType) getFirmType(classTy)).getPointsTo();

            for (var field : classTy.getFields().values()) {
                Type fieldType = getFirmType((Ty) this.resolution.bindingTypes().get(field).orElseThrow());
                Entity fieldEnt = new Entity(classType, field.getIdentifier().toString(), fieldType);
                this.entities.set(field, fieldEnt);
            }

            for (var m: classTy.getMethods().values()) {
                if (m instanceof DefinedMethod method){
                    var name = method.getName();
                    var type = getMethodType(method);
                    Entity methodEnt = new Entity(globalType, m.getLinkerName(), type);
                    this.entities.set(method.getAstMethod(), methodEnt);
                }
            }
        }

        for (var intrinsicMethod : IntrinsicMethod.ALL_INTRINSIC_METHODS) {
            var methodType = getIntrinsicMethodType(intrinsicMethod);
            var entity = new Entity(globalType, intrinsicMethod.getLinkerName(), methodType);
            this.intrinsicEntities.put(intrinsicMethod, entity);
        }

        for (var classTy : this.resolution.classes()) {
            for (var methodDef : classTy.getMethods().values()) {
                // We don't generate code for intrinsic methods.
                if (methodDef instanceof DefinedMethod definedMethod) {
                    Graph graph = genGraphForMethod(definedMethod);
                    Dump.dumpGraph(graph, methodDef.getName());
                }
            }
        }

        try {
            Dump.dumpTypeGraph("types.vcg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
