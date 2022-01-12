package compiler;

import compiler.ast.*;
import compiler.semantic.AstData;
import compiler.semantic.ConstantFolding;
import compiler.semantic.SparseAstData;
import compiler.semantic.WellFormed;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.semantic.resolution.NameResolution;
import compiler.types.*;
import firm.Type;
import firm.*;
import firm.bindings.binding_ircons;
import firm.nodes.Block;
import firm.nodes.Node;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("DuplicateBranchesInSwitch")
public class Translation {

    private final FrontendResult frontend;

    private final Map<Ty, Type> firmTypes;
    private final List<StructType> allCreatedStructFirmTypes;

    private final AstData<Integer> variableId; // maps variable definitions to their firm variable ids.
    private final AstData<Entity> entities; // maps method and fields to their respective entity.
    private final Map<IntrinsicMethod, Entity> intrinsicEntities;
    private final Entity allocFunctionEntity;
    private final List<Node> returns;
    private final CompoundType globalType;
    private int thisVariableId; // firm variable id for implicit this parameter
    private int nextVariableId;
    private boolean emitJump;

    private Construction construction;

    static {
        Firm.VERSION = Firm.FirmVersion.DEBUG;
    }

    public Translation(FrontendResult frontend) {
        this.frontend = frontend;

        //Backend.option("dump=all");

        Firm.init("x86_64-linux-gnu", new String[]{"pic=1"});
        this.firmTypes = new HashMap<>();
        this.allCreatedStructFirmTypes = new ArrayList<>();
        this.variableId = new SparseAstData<>();
        this.entities = new SparseAstData<>();
        this.intrinsicEntities = new HashMap<>();
        this.returns = new ArrayList<>();
        this.globalType = firm.Program.getGlobalType();

        this.allocFunctionEntity = createAllocFunctionEntity();

        this.nextVariableId = 0;

        this.emitJump = true;
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

    private Entity createAllocFunctionEntity() {
        var argType = new PrimitiveType(Mode.getIs());
        var retType = new PrimitiveType(Mode.getP());
        var type = new MethodType(new Type[]{argType, argType}, new Type[]{retType});
        return new Entity(this.globalType, "__builtin_alloc_function__", type);
    }

    private Type getFirmType(Ty type) {
        return switch (type) {
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
                    this.allCreatedStructFirmTypes.add(firmType);
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
    }

    private Node translateLiteral(AstNode literal) {
        return switch (literal) {
            case BoolLiteral lit -> construction.newConst(lit.getValue() ? 1 : 0, Mode.getBu());
            case IntLiteral lit -> {
                var value = frontend.constants().get(lit).orElseThrow();
                yield construction.newConst(value, Mode.getIs());
            }
            default -> throw new AssertionError("translateLiteral called with " + literal);
        };
    }

    private Node translateDiv(Node lhs, Node rhs) {
        Node negConst = construction.newConst(-2147483648, Mode.getIs());
        Node cmp = createCompareBinOpNode(BinaryOpExpression.BinaryOp.Equal, lhs, negConst);

        Block isMinBlock = construction.newBlock();
        Block divBlock = construction.newBlock();
        Block nextBlock = construction.newBlock();

        translateCondBoolCmp(cmp, isMinBlock, divBlock);

        isMinBlock.mature();
        construction.setCurrentBlock(isMinBlock);
        Node minusOne = construction.newConst(-1, Mode.getIs());
        cmp = createCompareBinOpNode(BinaryOpExpression.BinaryOp.Equal, rhs, minusOne);
        translateCondBoolCmp(cmp, nextBlock, divBlock);

        divBlock.mature();
        construction.setCurrentBlock(divBlock);

        Node div = construction.newDiv(construction.getCurrentMem(), lhs, rhs, binding_ircons.op_pin_state.op_pin_state_pinned);
        Node memProj = construction.newProj(div, Mode.getM(), 0);
        Node divRes = construction.newProj(div, Mode.getIs(), 1);
        construction.setCurrentMem(memProj);
        Node divJmp = construction.newJmp();

        nextBlock.addPred(divJmp);
        nextBlock.mature();
        construction.setCurrentBlock(nextBlock);
        Node resPhi = construction.newPhi(new Node[]{negConst, divRes}, Mode.getIs());

        return resPhi;
    }

    private Node translateArithBinOp(BinaryOpExpression expr) {
        Node lhs = translateExpr(expr.getLhs());
        Node rhs = translateExpr(expr.getRhs());
        return switch (expr.getOperator()) {
            case Addition -> construction.newAdd(lhs, rhs);
            case Subtraction -> construction.newSub(lhs, rhs);
            case Multiplication -> construction.newMul(lhs, rhs);
            case Division -> translateDiv(lhs, rhs);
            case Modulo -> {
                Node div = construction.newMod(construction.getCurrentMem(), lhs, rhs, binding_ircons.op_pin_state.op_pin_state_pinned);
                Node memProj = construction.newProj(div, Mode.getM(), 0);
                Node resProj = construction.newProj(div, Mode.getIs(), 1);
                construction.setCurrentMem(memProj);
                yield resProj;
            }
            default -> throw new AssertionError("not an arith bitop");
        };
    }

    /* returns node with mode B */
    private Node createCompareBinOpNode(BinaryOpExpression expr) {
        Node lhs = translateExpr(expr.getLhs());
        Node rhs = translateExpr(expr.getRhs());
        return createCompareBinOpNode(expr.getOperator(), lhs, rhs);
    }

    private Node createCompareBinOpNode(BinaryOpExpression.BinaryOp op, Node lhs, Node rhs) {
        return switch (op) {
            case Equal -> construction.newCmp(lhs, rhs, Relation.Equal);
            case NotEqual -> {
                var equalNode = construction.newCmp(lhs, rhs, Relation.Equal);
                yield construction.newNot(equalNode);
            }
            case Less -> construction.newCmp(lhs, rhs, Relation.Less);
            case LessEqual -> construction.newCmp(lhs, rhs, Relation.LessEqual);
            case Greater -> construction.newCmp(lhs, rhs, Relation.Greater);
            case GreaterEqual -> construction.newCmp(lhs, rhs, Relation.GreaterEqual);
            default -> throw new AssertionError("invalid boolean op");
        };
    }

    private Node translateBinOpExpr(BinaryOpExpression expr) {
        return switch (expr.getOperator()) {
            case Addition, Subtraction, Multiplication, Division, Modulo -> translateArithBinOp(expr);
            case Equal, NotEqual, Less, LessEqual, Greater, GreaterEqual -> {
                var node = createCompareBinOpNode(expr);
                yield translateCondBoolToByteBool(node);
            }
            case And, Or -> {
                Block trueBlock = construction.newBlock();
                Block falseBlock = construction.newBlock();

                translateExprWithShortcircuit(expr, trueBlock, falseBlock);

                trueBlock.mature();
                falseBlock.mature();

                construction.setCurrentBlock(trueBlock);
                var trueJmp = construction.newJmp();

                construction.setCurrentBlock(falseBlock);
                var falseJmp = construction.newJmp();

                Block phiBlock = construction.newBlock();

                phiBlock.addPred(trueJmp);
                phiBlock.addPred(falseJmp);
                phiBlock.mature();

                construction.setCurrentBlock(phiBlock);
                var constOne = construction.newConst(1, Mode.getBu());
                var constZero = construction.newConst(0, Mode.getBu());
                yield construction.newPhi(new Node[] { constOne, constZero }, Mode.getBu());
            }
        };
    }

    private void translateCondBoolCmp(Node cmp, Block trueBlock, Block falseBlock) {
        Node ifN = construction.newCond(cmp);

        Node trueProj = construction.newProj(ifN, Mode.getX(), 1);
        trueBlock.addPred(trueProj);

        Node falseProj = construction.newProj(ifN, Mode.getX(), 0);
        falseBlock.addPred(falseProj);

    }

    private void translateCompareWithShortcircuit(BinaryOpExpression expr, Block trueBlock, Block falseBlock) {
        var cmpNode = createCompareBinOpNode(expr);
        translateCondBoolCmp(cmpNode, trueBlock, falseBlock);
    }

    private void translateBinOpWithShortcircuit(BinaryOpExpression expr, Block trueBlock, Block falseBlock) {
        switch (expr.getOperator()) {
            case And -> {
                Block rightBlock = construction.newBlock();
                translateExprWithShortcircuit(expr.getLhs(), rightBlock, falseBlock);
                rightBlock.mature();
                construction.setCurrentBlock(rightBlock);
                translateExprWithShortcircuit(expr.getRhs(), trueBlock, falseBlock);
            }
            case Or -> {
                Block rightBlock = construction.newBlock();
                translateExprWithShortcircuit(expr.getLhs(), trueBlock, rightBlock);
                rightBlock.mature();
                construction.setCurrentBlock(rightBlock);
                translateExprWithShortcircuit(expr.getRhs(), trueBlock, falseBlock);
            }
            case Equal -> translateCompareWithShortcircuit(expr, trueBlock, falseBlock);
            case NotEqual -> translateCompareWithShortcircuit(expr, trueBlock, falseBlock);
            case Less -> translateCompareWithShortcircuit(expr, trueBlock, falseBlock);
            case LessEqual -> translateCompareWithShortcircuit(expr, trueBlock, falseBlock);
            case Greater -> translateCompareWithShortcircuit(expr, trueBlock, falseBlock);
            case GreaterEqual -> translateCompareWithShortcircuit(expr, trueBlock, falseBlock);
            default -> throw new AssertionError("untranslatable op");
        }
    }

    private Node translateCondBoolToByteBool(Node node) {
        assert node.getMode().equals(Mode.getb());

        Node trueConst = construction.newConst(1, Mode.getBu());
        Node falseConst = construction.newConst(0, Mode.getBu());

        Node condNode = construction.newCond(node);
        Node trueProj = construction.newProj(condNode, Mode.getX(), 1);
        Node falseProj = construction.newProj(condNode, Mode.getX(), 0);

        Block nextBlock = construction.newBlock();
        nextBlock.addPred(trueProj);
        nextBlock.addPred(falseProj);
        nextBlock.mature();

        construction.setCurrentBlock(nextBlock);
        return construction.newPhi(new Node[]{trueConst, falseConst}, Mode.getBu());
    }

    private void translateByteBoolToShortcircuit(Node input, Block trueBlock, Block falseBlock) {
        Node constOne = construction.newConst(1, Mode.getBu());
        Node cmp = construction.newCmp(input, constOne, Relation.Equal);
        Node ifN = construction.newCond(cmp);

        Node trueProj = construction.newProj(ifN, Mode.getX(), 1);
        trueBlock.addPred(trueProj);

        Node falseProj = construction.newProj(ifN, Mode.getX(), 0);
        falseBlock.addPred(falseProj);
    }

    private void translateExprWithShortcircuit(Expression root, Block trueBlock, Block falseBlock) {
        switch (root) {
            case BinaryOpExpression expr -> translateBinOpWithShortcircuit(expr, trueBlock, falseBlock);
            case UnaryExpression expr -> {
                assert expr.getOperator() == UnaryExpression.UnaryOp.LogicalNot;
                translateExprWithShortcircuit(expr.getExpression(), falseBlock, trueBlock);
            }
            case Reference expr -> {
                Node n = translateExpr(expr);
                translateByteBoolToShortcircuit(n, trueBlock, falseBlock);
            }
            case BoolLiteral expr -> {
                Node n = translateExpr(expr);
                translateByteBoolToShortcircuit(n, trueBlock, falseBlock);
            }
            case AssignmentExpression expr -> {
                Node n = translateExpr(expr);
                translateByteBoolToShortcircuit(n, trueBlock, falseBlock);
            }
            case FieldAccessExpression expr -> {
                Node n = translateExpr(expr);
                translateByteBoolToShortcircuit(n, trueBlock, falseBlock);
            }
            case ArrayAccessExpression expr -> {
                Node n = translateExpr(expr);
                translateByteBoolToShortcircuit(n, trueBlock, falseBlock);
            }
            case MethodCallExpression expr -> {
                Node n = translateExpr(expr);
                translateByteBoolToShortcircuit(n, trueBlock, falseBlock);
            }
            default -> throw new AssertionError("translateCond with non-cond expr");
        }
    }

    private Node translateUnaryOp(UnaryExpression expr) {
        Node rhs = translateExpr(expr.getExpression());
        return switch (expr.getOperator()) {
            case LogicalNot -> {
                Node oneConst = construction.newConst(1, Mode.getBu());
                yield construction.newEor(oneConst, rhs);
            }
            case Negate -> construction.newMinus(rhs);
        };
    }

    private Node translateFieldExprToLValue(Node targetNode, Expression expr) {
        assert targetNode.getMode().equals(Mode.getP());

        Field field;
        if (expr instanceof FieldAccessExpression fieldAccessExpression) {
            field = frontend.definitions().getField(fieldAccessExpression).orElseThrow();
        } else if (expr instanceof Reference ref) {
            field = (Field) frontend.definitions().getReference(ref).orElseThrow();
        } else {
            throw new AssertionError("Unrecheable");
        }

        var memberEntity = this.entities.get(field).orElseThrow();

        return construction.newMember(targetNode, memberEntity);
    }

    private Node translateFieldAccessExpr(Node targetNode, Expression expr) {
        var fieldPtr = translateFieldExprToLValue(targetNode, expr);

        var mem = construction.getCurrentMem();
        var exprTy = (Ty)frontend.expressionTypes().get(expr).orElseThrow();
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

    private Node translateArrayAccessExprLValue(ArrayAccessExpression expr) {
        var targetNode = translateExpr(expr.getTarget());
        assert targetNode.getMode().equals(Mode.getP());

        var indexNode = translateExpr(expr.getIndexExpression());

        var exprTy = (ArrayTy)frontend.expressionTypes().get(expr.getTarget()).orElseThrow();
        var childTy = exprTy.getChildTy();
        var childFirmType = getFirmType(childTy);

        var objectSize = construction.newSize(Mode.getIs(), childFirmType);

        var byteIndexNode = construction.newConv(construction.newMul(indexNode, objectSize), Mode.getLs());

        return construction.newAdd(targetNode, byteIndexNode);
    }

    private Optional<Node> translateMethodCallExpression(MethodCallExpression expr) {
        var methodDef = frontend.definitions().getMethod(expr).orElseThrow();

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
            case BinaryOpExpression expr -> translateBinOpExpr(expr);
            case FieldAccessExpression expr -> {
                var targetNode = translateExpr(expr.getTarget());
                yield translateFieldAccessExpr(targetNode, expr);
            }
            case AssignmentExpression expr -> {
                switch (expr.getLvalue()) {
                    case Reference var -> {
                        var rhs = translateExpr(expr.getRvalue());
                        var definition = frontend.definitions().getReference(var).orElseThrow();

                        if (definition instanceof LocalVariableDeclarationStatement || definition instanceof Parameter) {
                            var firmVarId = variableId.get((AstNode) definition).orElseThrow();
                            construction.setVariable(firmVarId, rhs);
                        } else {
                            assert definition instanceof Field;
                            var thisNode = construction.getVariable(this.thisVariableId, Mode.getP());
                            translateFieldAssignment(thisNode, var, rhs);
                        }

                        yield rhs;
                    }
                    case FieldAccessExpression fieldAccess -> {
                        var targetNode = translateExpr(fieldAccess.getTarget());
                        var rhs = translateExpr(expr.getRvalue());

                        translateFieldAssignment(targetNode, fieldAccess, rhs);

                        yield rhs;
                    }
                    case ArrayAccessExpression arrayAccess -> {
                        var arrayFieldPtr = translateArrayAccessExprLValue(arrayAccess);
                        // Important: rvalue has to be executed before lvalue.
                        var rhs = translateExpr(expr.getRvalue());

                        var mem = construction.getCurrentMem();
                        var store = construction.newStore(mem, arrayFieldPtr, rhs);
                        var memProj = construction.newProj(store, Mode.getM(), 0);
                        construction.setCurrentMem(memProj);

                        yield rhs;
                    }
                    default -> throw new AssertionError("Not an lvalue");
                }
            }
            case ArrayAccessExpression expr -> {
                var arrayFieldPtr = translateArrayAccessExprLValue(expr);

                var childFirmType = getFirmType((Ty)frontend.expressionTypes().get(expr).orElseThrow());

                var mem = construction.getCurrentMem();
                var loadNode = construction.newLoad(mem, arrayFieldPtr, childFirmType.getMode());

                var memProj = construction.newProj(loadNode, Mode.getM(), 0);
                construction.setCurrentMem(memProj);

                yield construction.newProj(loadNode, childFirmType.getMode(), 1);
            }
            case MethodCallExpression expr -> {
                var node = translateMethodCallExpression(expr);

                yield node.orElseThrow(() -> new AssertionError("MethodCallExpression of void methods can only be directly after ExpressionStatements."));
            }
            case NewArrayExpression expr -> {
                var exprTy = (Ty)frontend.expressionTypes().get(expr).orElseThrow();
                var firmTy = getFirmType(exprTy);
                assert firmTy.getMode().equals(Mode.getP());
                var childType = ((PointerType) firmTy).getPointsTo();

                var typeSize = construction.newSize(Mode.getIs(), childType);
                var arrayLength = translateExpr(expr.getFirstDimensionSize());

                var mem = construction.getCurrentMem();
                var addr = construction.newAddress(this.allocFunctionEntity);
                var callNode = construction.newCall(mem, addr, new Node[]{typeSize, arrayLength}, this.allocFunctionEntity.getType());

                var memProj = construction.newProj(callNode, Mode.getM(), 0);
                construction.setCurrentMem(memProj);

                var returnValuesProj = construction.newProj(callNode, Mode.getT(), 1);

                yield construction.newProj(returnValuesProj, Mode.getP(), 0);
            }
            case NewObjectExpression expr -> {
                var exprTy = (Ty)frontend.expressionTypes().get(expr).orElseThrow();
                var firmTy = getFirmType(exprTy);
                assert firmTy.getMode().equals(Mode.getP());
                var classType = ((PointerType) firmTy).getPointsTo();

                var size = construction.newSize(Mode.getIs(), classType);
                var constantOne = construction.newConst(1, Mode.getIs());

                var mem = construction.getCurrentMem();
                var addr = construction.newAddress(this.allocFunctionEntity);
                var callNode = construction.newCall(mem, addr, new Node[]{size, constantOne}, this.allocFunctionEntity.getType());

                var memProj = construction.newProj(callNode, Mode.getM(), 0);
                construction.setCurrentMem(memProj);

                var returnValuesProj = construction.newProj(callNode, Mode.getT(), 1);

                yield construction.newProj(returnValuesProj, Mode.getP(), 0);
            }
            case NullExpression ignored -> construction.newConst(0, Mode.getP());
            case ThisExpression ignored -> construction.getVariable(this.thisVariableId, Mode.getP());
            case UnaryExpression expr -> translateUnaryOp(expr);
            case Reference expr -> {
                var definition = frontend.definitions().getReference(expr).orElseThrow();

                if (definition instanceof LocalVariableDeclarationStatement || definition instanceof Parameter) {
                    var defType = (Ty) frontend.expressionTypes().get(expr).orElseThrow();
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
        this.emitJump = true;
        switch (statement) {
            case EmptyStatement ignored -> {}
            case ExpressionStatement stmt -> {
                if (stmt.getExpression() instanceof MethodCallExpression methodCall) {
                    translateMethodCallExpression(methodCall);
                } else {
                    translateExpr(stmt.getExpression());
                }
            }
            case IfStatement stmt -> {
                // TODO: Don't place jmp if there is a return
                Block trueBlock = construction.newBlock();
                Block falseBlock = construction.newBlock();

                translateExprWithShortcircuit(stmt.getCondition(), trueBlock, falseBlock);

                trueBlock.mature();
                construction.setCurrentBlock(trueBlock);
                translateStatement(stmt.getThenBody());
                var trueNeedsJmp = emitJump;
                Node trueJmp = construction.newJmp();

                if (stmt.getElseBody().isEmpty()) {
                    // Simple if
                    construction.setCurrentBlock(falseBlock);
                    if (trueNeedsJmp) {
                        falseBlock.addPred(trueJmp);
                        falseBlock.mature();
                    } else {
                        falseBlock.mature();
                        this.emitJump = true;
                    }

                } else {
                    // We have an else part
                    falseBlock.mature();
                    construction.setCurrentBlock(falseBlock);

                    translateStatement(stmt.getElseBody().get());
                    boolean falseNeedsJmp = emitJump;
                    Node falseJmp = construction.newJmp();

                    Block followingBlock = construction.newBlock();
                    if (trueNeedsJmp) {
                        followingBlock.addPred(trueJmp);
                    }
                    if (falseNeedsJmp) {
                        followingBlock.addPred(falseJmp);
                    }
                    followingBlock.mature();
                    construction.setCurrentBlock(followingBlock);
                    if (!trueNeedsJmp && !falseNeedsJmp) {
                        // This is a dead end
                        this.emitJump = false;
                    } else {
                        // This is absolutely necessary. Otherwise emitJump is set implicitly by
                        // the content of the else block. If its last statment changing the flag
                        // is a return, there may exist a path not covered by an early return,
                        // but the flag wrongly tells us not to create a jump.
                        this.emitJump = true;
                    }
                }
            }
            case LocalVariableDeclarationStatement stmt -> {
                var statementId = this.newVariableId();
                variableId.set(stmt, statementId);

                if (stmt.getInitializer().isPresent()) {
                    var node = translateExpr(stmt.getInitializer().get());
                    construction.setVariable(statementId, node);
                } else {
                    var ty = (Ty)frontend.bindingTypes().get(stmt).orElseThrow();
                    var firmType = getFirmType(ty);
                    var defaultValue = construction.newConst(0, firmType.getMode());
                    construction.setVariable(statementId, defaultValue);
                }
            }
            case ReturnStatement stmt -> {
                Node[] rhs = stmt.getExpression().map(expr -> new Node[]{translateExpr(expr)}).orElse(new Node[0]);
                var ret = construction.newReturn(construction.getCurrentMem(), rhs);
                this.returns.add(ret);
                this.emitJump = false;
            }
            case WhileStatement stmt -> {
                // TODO: Don't place jmp if there is a return
                Block headerBlock = construction.newBlock();
                Block bodyBlock = construction.newBlock();
                Block followingBlock = construction.newBlock();

                Node jmpToHead = construction.newJmp();
                construction.setCurrentBlock(headerBlock);
                headerBlock.addPred(jmpToHead);
                // Handle infinite loops: Force mem node creation, keep alive
                construction.getGraph().keepAlive(headerBlock);

                translateExprWithShortcircuit(stmt.getCondition(), bodyBlock, followingBlock);

                bodyBlock.mature();
                followingBlock.mature();
                construction.setCurrentBlock(bodyBlock);
                translateStatement(stmt.getBody());

                if (this.emitJump) {
                    Node loopJmp = construction.newJmp();
                    headerBlock.addPred(loopJmp);
                }
                headerBlock.mature();

                construction.setCurrentBlock(followingBlock);
            }
            case compiler.ast.Block block -> {
                for (var stmt : block.getStatements()) {
                    if (!frontend.isDeadStatement().get(stmt).orElse(false)) {
                        translateStatement(stmt);
                    }
                }
            }
        }
    }

    private Graph genGraphForMethod(DefinedMethod methodDef) {
        this.nextVariableId = 0;
        this.returns.clear();
        this.thisVariableId = -1;

        var name = methodDef.getName();
        var methodEnt = this.entities.get(methodDef.getAstMethod()).orElseThrow();

        var isMainMethod = methodDef == frontend.mainMethod();

        var numberLocalVars = frontend.variableCounts().get(methodDef.getAstMethod()).orElseThrow();
        var numberParameters = methodDef.getParameterTy().size();
        var numberFirmVars = numberLocalVars + numberParameters + (isMainMethod ? 0 : 1); // +1 is implicit this argument
        Graph graph = new Graph(methodEnt, numberFirmVars);

        construction = new Construction(graph);

        Node startNode = construction.getGraph().getStart();
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
            var ty = (Ty) frontend.bindingTypes().get(param).orElseThrow();

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

       translateStatement(body);

        if (this.emitJump) {
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

    public void translate(boolean dumpGraphs) {
        for (var classTy : frontend.classes()) {
            CompoundType classType = (CompoundType) ((PointerType) getFirmType(classTy)).getPointsTo();

            for (var field : classTy.getFields().values()) {
                Type fieldType = getFirmType((Ty) frontend.bindingTypes().get(field).orElseThrow());
                Entity fieldEnt = new Entity(classType, field.getIdentifier().toString(), fieldType);
                this.entities.set(field, fieldEnt);
            }

            for (var m: classTy.getMethods().values()) {
                if (m instanceof DefinedMethod method){
                    var type = getMethodType(method);
                    Entity methodEnt = new Entity(globalType, method == frontend.mainMethod() ? "__MiniJava_Main__" : m.getLinkerName(), type);
                    this.entities.set(method.getAstMethod(), methodEnt);
                }
            }
        }

        for (var intrinsicMethod : IntrinsicMethod.ALL_INTRINSIC_METHODS) {
            var methodType = getIntrinsicMethodType(intrinsicMethod);
            var entity = new Entity(globalType, intrinsicMethod.getLinkerName(), methodType);
            this.intrinsicEntities.put(intrinsicMethod, entity);
        }

        ArrayList<Graph> graphs = new ArrayList<>();
        ArrayDeque<String> names = new ArrayDeque<>();
        for (var classTy : frontend.classes()) {
            for (var methodDef : classTy.getMethods().values()) {
                // We don't generate code for intrinsic methods.
                if (methodDef instanceof DefinedMethod definedMethod) {
                    Graph graph = genGraphForMethod(definedMethod);
                    graphs.add(Optimization.constantFolding(graph));
                    names.add(methodDef.getName());


                }
            }
        }
        graphs.forEach(graph -> {
            InliningOptimization inliningOptimization = new InliningOptimization(graph);
            inliningOptimization.collectNodes();
            System.out.println("DONE");
            if (dumpGraphs) {
                Dump.dumpGraph(graph, names.pop());
            }
        });

        if (dumpGraphs) {
            try {
                Dump.dumpTypeGraph("types.vcg");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (firm.Type firmType : allCreatedStructFirmTypes) {
            if (firmType instanceof StructType st) {
                st.layoutFields();
                st.finishLayout();
            }
        }
    }
}
