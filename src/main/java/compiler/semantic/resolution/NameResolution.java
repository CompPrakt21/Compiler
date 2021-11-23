package compiler.semantic.resolution;

import compiler.ast.Class;
import compiler.ast.*;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.semantic.AstData;
import compiler.semantic.DenseAstData;
import compiler.types.*;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NameResolution {

    private SymbolTable<VariableDefinition> symbols;
    private Class currentClass;
    private Method currentMethod;

    private final Definitions definitions; // Maps every ast node with an identifier to the ast node that defines it.
    private final AstData<TyResult> expressionTypes; // Types of all expressions

    private record ClassInfo(ClassDefinition definition, ClassTy type) {
    }

    // Identifier -> (Class, ClassTy)
    private final Map<String, ClassInfo> classInfo;

    private final AstData<TyResult> bindingTypes; // Field type, Method return type, Parameter types or Local var declarations

    private final Optional<CompilerMessageReporter> reporter;

    private boolean successful;

    @SuppressWarnings("unused")
    public NameResolution() {
        this(Optional.empty());
    }

    public NameResolution(CompilerMessageReporter reporter) {
        this(Optional.of(reporter));
    }

    private NameResolution(Optional<CompilerMessageReporter> reporter) {
        this.symbols = null;
        this.currentClass = null;
        this.currentMethod = null;

        this.definitions = new Definitions();
        this.expressionTypes = new DenseAstData<>();
        this.bindingTypes = new DenseAstData<>();

        this.classInfo = new HashMap<>();

        this.reporter = reporter;

        this.successful = true;
    }

    public record NameResolutionResult(Definitions definitions,
                                       AstData<TyResult> types,
                                       boolean successful) {
    }

    public static NameResolutionResult performNameResolution(Program program, CompilerMessageReporter reporter) {
        var resolution = new NameResolution(reporter);

        resolution.addIntrinsicClasses();

        resolution.globalNameResolution(program);

        for (Class klass : program.getClasses()) {
            resolution.currentClass = klass;

            for (Method m : klass.getMethods()) {
                resolution.resolveMethod(m);
            }
        }

        return new NameResolutionResult(
                resolution.definitions,
                resolution.expressionTypes,
                resolution.successful
        );
    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
        this.successful = false;
    }

    private void addIntrinsicClasses() {
        this.classInfo.put(IntrinsicClass.STRING_CLASS.getName(), new ClassInfo(IntrinsicClass.STRING_CLASS, new ClassTy(IntrinsicClass.STRING_CLASS)));
    }

    private void globalNameResolution(Program program) {
        for (Class klass : program.getClasses()) {

            var name = klass.getIdentifier().getContent();

            if (this.classInfo.containsKey(name)) {
                var firstDef = this.classInfo.get(name);
                reportError(new MultipleUseOfSameClassName(firstDef.definition, klass));
                continue;
            }

            var classDef = new DefinedClass(klass);
            this.classInfo.put(name, new ClassInfo(classDef, new ClassTy(classDef)));
        }

        // Only look at collected classes (thereby ignoring classes which reuse an already defined class name)
        for (String className : classInfo.keySet()) {
            var classInfo = this.classInfo.get(className);

            DefinedClass classDef;
            if (classInfo.definition instanceof DefinedClass dc) {
                classDef = dc;
            } else {
                continue;
            }

            var klass = classDef.getAstClass();

            for (Method m : klass.getMethods()) {

                var methodName = m.getIdentifier().getContent();
                var maybeAlreadyUsed = classDef.searchMethod(methodName);
                if (maybeAlreadyUsed.isPresent()) {
                    assert maybeAlreadyUsed.get() instanceof DefinedMethod;
                    var firstUse = ((DefinedMethod) maybeAlreadyUsed.get()).getAstMethod();
                    reportError(new MultipleUseOfSameMemberName(firstUse, m));
                }

                var returnType = m.getReturnType();
                this.resolveType(returnType);
                var returnTy = this.fromAstType(returnType);

                this.bindingTypes.set(m, returnTy);

                if (!(returnTy instanceof Ty) && !(returnTy instanceof VoidTy)) {
                    reportError(new UnresolveableMemberType(m));
                }

                List<TyResult> paramTys = new ArrayList<>();
                for (Parameter param : m.getParameters()) {
                    this.resolveType(param.getType());
                    var paramType = this.fromAstType(param.getType());

                    switch (paramType) {
                        case VoidTy ignored -> reportError(new IllegalMethodParameterType(param, IllegalMethodParameterType.Reason.VOID));
                        case UnresolveableTy ignored -> reportError(new IllegalMethodParameterType(param, IllegalMethodParameterType.Reason.UNRESOLVEABLE));
                        case Ty ignored -> {
                        }
                    }

                    paramTys.add(paramType);

                    this.bindingTypes.set(param, paramType);
                }

                classDef.addMethod(new DefinedMethod(m, classDef, returnTy, paramTys));
            }

            for (Field f : klass.getFields()) {

                var fieldName = f.getIdentifier().getContent();
                var maybeAlreadyUsed = classDef.searchField(fieldName);
                if (maybeAlreadyUsed.isPresent()) {
                    var firstUse = maybeAlreadyUsed.get();
                    reportError(new MultipleUseOfSameMemberName(firstUse, f));
                    continue;
                }

                var type = f.getType();
                this.resolveType(type);
                var ty = this.fromAstType(type);

                this.bindingTypes.set(f, ty);

                if (!(ty instanceof Ty)) {
                    reportError(new UnresolveableMemberType(f));
                }

                classDef.addField(f);
            }
        }
    }

    private void resolveMethod(Method method) {
        this.symbols = new SymbolTable<>();

        resolveType(method.getReturnType());

        for (Field f : this.currentClass.getFields()) {
            symbols.insert(f.getIdentifier().getContent(), f);
        }

        symbols.enterScope();

        for (var param : method.getParameters()) {

            var maybeAlreadyExists = symbols.lookupDefinition(param.getIdentifier().getContent());
            if (maybeAlreadyExists.isPresent() && maybeAlreadyExists.get() instanceof Parameter firstParam) {
                reportError(new DuplicateParameterName(firstParam, param));
            }

            symbols.insert(param.getIdentifier().getContent(), param);
        }

        this.currentMethod = method;
        resolveStatement(method.getBody());
    }

    private void resolveStatement(Statement statement) {
        switch (statement) {
            case Block block -> {
                symbols.enterScope();

                for (var stmt : block.getStatements()) {
                    resolveStatement(stmt);
                }

                symbols.leaveScope();
            }
            case EmptyStatement ignored -> {
            }
            case IfStatement ifStmt -> {
                resolveExpression(ifStmt.getCondition());
                resolveStatement(ifStmt.getThenBody());

                var elseBody = ifStmt.getElseBody();
                elseBody.ifPresent(this::resolveStatement);

                var conditionTy = this.expressionTypes.get(ifStmt.getCondition()).get();
                if (!(conditionTy instanceof BoolTy || conditionTy instanceof UnresolveableTy)) {
                    reportError(new IfConditionTypeMismatch(ifStmt, conditionTy));
                }
            }
            case ExpressionStatement exprStmt -> resolveExpression(exprStmt.getExpression());
            case WhileStatement whileStmt -> {
                resolveExpression(whileStmt.getCondition());
                resolveStatement(whileStmt.getBody());

                var conditionTy = this.expressionTypes.get(whileStmt.getCondition()).get();
                if (!(conditionTy instanceof BoolTy || conditionTy instanceof UnresolveableTy)) {
                    reportError(new WhileConditionTypeMismatch(whileStmt, conditionTy));
                }
            }
            case ReturnStatement retStmt -> {
                retStmt.getExpression().ifPresent(this::resolveExpression);

                var returnType = this.bindingTypes.get(this.currentMethod).get();

                switch (returnType) {
                    case Ty expectedReturnTy -> {
                        if (retStmt.getExpression().isPresent()) {
                            var retExpr = retStmt.getExpression().get();
                            var retExprTyRes = this.expressionTypes.get(retExpr).get();

                            if (retExprTyRes instanceof Ty retExprTy) {
                                if (!(retExprTy.comparable(expectedReturnTy))) {
                                    reportError(new ReturnStatementErrors.TypeMismatch(currentMethod, expectedReturnTy, retStmt, retExprTy));
                                }
                            } else if (retExprTyRes instanceof VoidTy) {
                                reportError(new ReturnStatementErrors.TypeMismatch(this.currentMethod, expectedReturnTy, retStmt, retExprTyRes));
                            }
                        } else {
                            reportError(new ReturnStatementErrors.MissingReturnExpr(this.currentMethod, retStmt, expectedReturnTy));
                        }
                    }
                    case VoidTy ignored -> {
                        assert returnType instanceof VoidTy;

                        if (retStmt.getExpression().isPresent()) {
                            reportError(new ReturnStatementErrors.UnexpectedReturnExpr(this.currentMethod, retStmt));
                        }
                    }
                    case UnresolveableTy ignored -> { /* error already reported */}
                }
            }
            case LocalVariableDeclarationStatement declStmt -> {
                var localName = declStmt.getIdentifier().getContent();

                resolveType(declStmt.getType());

                var maybeAlreadyDefined = this.symbols.lookupDefinition(localName);
                if (maybeAlreadyDefined.isPresent() && (maybeAlreadyDefined.get() instanceof LocalVariableDeclarationStatement || maybeAlreadyDefined.get() instanceof Parameter)) {
                    reportError(new IllegalLocalVariableShadowing(maybeAlreadyDefined.get(), declStmt));
                } else {
                    symbols.insert(localName, declStmt);
                }

                var bindingTy = this.fromAstType(declStmt.getType());
                this.bindingTypes.set(declStmt, bindingTy);

                declStmt.getInitializer().ifPresent(this::resolveExpression);

                switch (bindingTy) {
                    case Ty ty -> {
                        if (declStmt.getInitializer().isPresent()) {
                            var initTyRes = this.expressionTypes.get(declStmt.getInitializer().get()).get();

                            if (initTyRes instanceof VoidTy) {
                                reportError(new LocalDeclarationErrors.TypeMismatch(declStmt, ty, initTyRes));
                            } else if (initTyRes instanceof Ty initTy && !initTy.comparable(ty)) {
                                reportError(new LocalDeclarationErrors.TypeMismatch(declStmt, ty, initTy));
                            }
                        }
                    }
                    case VoidTy ignored -> reportError(new LocalDeclarationErrors.VoidType(declStmt));
                    case UnresolveableTy ignored -> reportError(new LocalDeclarationErrors.UnresolveableType(declStmt));
                }
            }
        }

    }

    private void resolveMethodCallWithTarget(MethodCallExpression methodCall, TyResult targetType) {
        if (targetType instanceof ClassTy classTy) {

            var classDef = classTy.getDefinition();

            var methodDef = classDef.searchMethod(methodCall.getIdentifier().getContent());
            if (methodDef.isPresent()) {
                this.definitions.setMethod(methodCall, methodDef.get());

                var returnType = methodDef.get().getReturnTy();

                this.expressionTypes.set(methodCall, returnType);
            } else {
                reportError(new UnresolveableMemberAccess(classTy.getDefinition(), methodCall));
                this.expressionTypes.set(methodCall, new UnresolveableTy());
            }
        } else {
            reportError(new MemberAccessOnNonClassType(methodCall, targetType));
            this.expressionTypes.set(methodCall, new UnresolveableTy());
        }
    }

    private void typeCheckSimpleBinaryExpression(BinaryOpExpression binaryOp, Ty expectedOperandTy, Ty
            operationTy, Ty lhsTy, Ty rhsTy) {
        Optional<Ty> lhs = expectedOperandTy.equals(lhsTy) ? Optional.empty() : Optional.of(lhsTy);
        Optional<Ty> rhs = expectedOperandTy.equals(rhsTy) ? Optional.empty() : Optional.of(rhsTy);

        if (lhs.isPresent() || rhs.isPresent()) {
            reportError(new BinaryExpressionTypeMismatch.InvalidTypesForOperator(binaryOp, expectedOperandTy, lhs, rhs));
        }
        this.expressionTypes.set(binaryOp, operationTy);
    }

    private void typecheckBinaryExpression(BinaryOpExpression binaryOp) {
        var lhsTyRes = this.expressionTypes.get(binaryOp.getLhs()).get();
        var rhsTyRes = this.expressionTypes.get(binaryOp.getRhs()).get();

        if (lhsTyRes instanceof UnresolveableTy || rhsTyRes instanceof UnresolveableTy) {

            this.expressionTypes.set(binaryOp, new UnresolveableTy());

        } else if (lhsTyRes instanceof VoidTy || rhsTyRes instanceof VoidTy) {

            Optional<TyResult> lhs = lhsTyRes instanceof VoidTy ? Optional.empty() : Optional.of(lhsTyRes);
            Optional<TyResult> rhs = rhsTyRes instanceof VoidTy ? Optional.empty() : Optional.of(rhsTyRes);
            reportError(new BinaryExpressionTypeMismatch.VoidOperand(binaryOp, lhs, rhs));
            this.expressionTypes.set(binaryOp, new UnresolveableTy());

        } else if (lhsTyRes instanceof Ty lhsTy && rhsTyRes instanceof Ty rhsTy) {

            switch (binaryOp.getOperator()) {
                case Addition, Subtraction, Multiplication, Division, Modulo -> typeCheckSimpleBinaryExpression(binaryOp, new IntTy(), new IntTy(), lhsTy, rhsTy);
                case Greater, GreaterEqual, Less, LessEqual -> typeCheckSimpleBinaryExpression(binaryOp, new IntTy(), new BoolTy(), lhsTy, rhsTy);
                case Equal, NotEqual -> {
                    if (!lhsTy.comparable(rhsTy)) {
                        reportError(new BinaryExpressionTypeMismatch.IncomparableTypes(binaryOp, lhsTy, rhsTy));
                    }
                    this.expressionTypes.set(binaryOp, new BoolTy());
                }
                case And, Or -> typeCheckSimpleBinaryExpression(binaryOp, new BoolTy(), new BoolTy(), lhsTy, rhsTy);
            }

        } else {
            throw new AssertionError("Unreacheable");
        }
    }

    private void resolveExpression(Expression expression) {
        switch (expression) {
            case AssignmentExpression assign -> {
                var lval = assign.getLvalue();
                var rval = assign.getRvalue();

                resolveExpression(lval);
                resolveExpression(rval);

                var lvalTyRes = this.expressionTypes.get(lval).get();
                var rvalTyRes = this.expressionTypes.get(rval).get();

                if (lvalTyRes instanceof Ty lvalTy && rvalTyRes instanceof Ty rvalTy) {
                    if (lvalTy.comparable(rvalTy)) {
                        this.expressionTypes.set(assign, lvalTy);
                    } else {
                        reportError(new AssignmentArgumentTypeMismatch(assign, lvalTy, rvalTy));
                        this.expressionTypes.set(assign, new UnresolveableTy());
                    }
                } else {
                    if (lvalTyRes instanceof VoidTy || rvalTyRes instanceof VoidTy) {
                        reportError(new AssignmentArgumentTypeMismatch(assign, lvalTyRes, rvalTyRes));
                    }
                    this.expressionTypes.set(assign, new UnresolveableTy());
                }

            }
            case BinaryOpExpression binaryOp -> {
                resolveExpression(binaryOp.getLhs());
                resolveExpression(binaryOp.getRhs());

                typecheckBinaryExpression(binaryOp);
            }
            case UnaryExpression unaryOp -> {
                resolveExpression(unaryOp.getExpression());

                Ty expectedTy = switch (unaryOp.getOperator()) {
                    case LogicalNot -> new BoolTy();
                    case Negate -> new IntTy();
                };

                var actualTy = this.expressionTypes.get(unaryOp.getExpression()).get();

                if (!expectedTy.equals(actualTy)) {
                    if (!(actualTy instanceof UnresolveableTy)) {
                        reportError(new UnaryExpressionTypeMismatch(unaryOp, expectedTy, actualTy));
                    }
                }

                this.expressionTypes.set(unaryOp, expectedTy);
            }
            case MethodCallExpression methodCall -> {
                var maybeIntrinsic = isIntrinsicMethodCall(methodCall);
                if (maybeIntrinsic.isPresent()) {
                    this.definitions.setMethod(methodCall, maybeIntrinsic.get());
                    this.expressionTypes.set(methodCall, maybeIntrinsic.get().getReturnTy());
                } else {

                    var target = methodCall.getTarget();

                    if (target.isPresent()) {
                        resolveExpression(target.get());

                        var targetType = this.expressionTypes.get(target.get()).get();

                        // If the target expression doesn't have a type an error was reported during resolve of this expression.
                        resolveMethodCallWithTarget(methodCall, targetType);
                    } else {
                        // Method call without target. => Target is this.
                        var targetType = this.getCurrentClassTy();
                        resolveMethodCallWithTarget(methodCall, targetType);
                    }
                }

                for (var arg : methodCall.getArguments()) {
                    resolveExpression(arg);
                }

                // Method arguments type checking
                var maybeMethod = this.definitions.getMethod(methodCall);
                if (maybeMethod.isPresent()) {
                    var methodDef = maybeMethod.get();

                    var argumentTypes = methodCall.getArguments().stream().map(arg -> this.expressionTypes.get(arg).get()).collect(Collectors.toList());
                    var paramTypes = methodDef.getParameterTy();

                    if (argumentTypes.size() != paramTypes.size()) {
                        reportError(new MethodParameterErrors.DifferentLength(methodDef, methodCall));
                    }

                    for (int i = 0; i < Math.min(argumentTypes.size(), paramTypes.size()); i += 1) {
                        var argTyRes = argumentTypes.get(i);
                        var paramTyRes = paramTypes.get(i);

                        if (paramTyRes instanceof Ty paramTy) {
                            if (argTyRes instanceof VoidTy || argTyRes instanceof Ty argTy && !argTy.comparable(paramTy)) {
                                reportError(new MethodParameterErrors.ArgumentTypeMismatch(methodDef, i, methodCall.getArguments().get(i), paramTy, argTyRes));
                            }
                        }
                    }
                }
            }
            case FieldAccessExpression fieldAccess -> {
                var target = fieldAccess.getTarget();
                resolveExpression(target);

                var targetType = this.expressionTypes.get(target).get();

                // If the target expression doesn't have a type an error was reported during resolve of this expression.
                if (targetType instanceof ClassTy classTy) {
                    var classDef = classTy.getDefinition();

                    var fieldDef = classDef.searchField(fieldAccess.getIdentifier().getContent());
                    if (fieldDef.isPresent()) {

                        this.definitions.setField(fieldAccess, fieldDef.get());

                        var ty = this.bindingTypes.get(fieldDef.get());

                        ty.ifPresent(value -> this.expressionTypes.set(fieldAccess, value));
                    } else {
                        reportError(new UnresolveableMemberAccess(classTy.getDefinition(), fieldAccess));
                        this.expressionTypes.set(fieldAccess, new UnresolveableTy());
                    }
                } else {
                    reportError(new MemberAccessOnNonClassType(fieldAccess, targetType));
                    this.expressionTypes.set(fieldAccess, new UnresolveableTy());
                }
            }
            case ArrayAccessExpression arrayAccess -> {
                resolveExpression(arrayAccess.getTarget());

                var targetType = this.expressionTypes.get(arrayAccess.getTarget()).get();

                if (targetType instanceof ArrayTy arrayTy) {
                    this.expressionTypes.set(arrayAccess, arrayTy.getChildTy());
                } else {
                    reportError(new ArrayAccessOnNonArrayType(arrayAccess, targetType));
                    this.expressionTypes.set(arrayAccess, new UnresolveableTy());
                }

                resolveExpression(arrayAccess.getIndexExpression());

                var indexTyRes = this.expressionTypes.get(arrayAccess.getIndexExpression()).get();
                if (!(indexTyRes instanceof IntTy || indexTyRes instanceof UnresolveableTy)) {
                    reportError(new GenericTypeMismatch(arrayAccess.getIndexExpression(), new IntTy(), indexTyRes));
                }
            }
            case NewArrayExpression newArray -> {
                resolveType(newArray.getType());

                var childType = this.fromAstType(newArray.getType());
                if (childType instanceof Ty childTy) {
                    this.expressionTypes.set(newArray, new ArrayTy(childTy, newArray.getDimensions()));
                } else {
                    reportError(new IllegalNewArrayExpression(childType, newArray));
                    this.expressionTypes.set(newArray, new UnresolveableTy());
                }

                resolveExpression(newArray.getFirstDimensionSize());

                var dimensionTyRes = this.expressionTypes.get(newArray.getFirstDimensionSize()).get();
                if (!(dimensionTyRes instanceof IntTy || dimensionTyRes instanceof UnresolveableTy)) {
                    reportError(new GenericTypeMismatch(newArray.getFirstDimensionSize(), new IntTy(), dimensionTyRes));
                }
            }
            case Reference ref -> {
                var def = this.symbols.lookupDefinition(ref.getIdentifier().getContent());
                if (def.isPresent()) {
                    this.definitions.setReference(ref, def.get());

                    var type = def.get().getType();

                    var ty = this.fromAstType(type);
                    // If the type is not valid, an error was emitted when resolving the definition.
                    this.expressionTypes.set(ref, ty);
                } else {
                    reportError(new UnresolveableReference(ref));
                    this.expressionTypes.set(ref, new UnresolveableTy());
                }
            }
            case NullExpression nullExpr -> this.expressionTypes.set(nullExpr, new NullTy());
            case BoolLiteral boolLit -> this.expressionTypes.set(boolLit, new BoolTy());
            case IntLiteral intLit -> this.expressionTypes.set(intLit, new IntTy());
            case ThisExpression thisExpr -> {
                this.expressionTypes.set(thisExpr, this.getCurrentClassTy());
            }
            case NewObjectExpression newObject -> {
                var classType = newObject.getType();

                resolveClassType(classType);

                var classTyRes = this.fromAstType(classType);

                // If its not Ty, then resolveClassType reported an error.
                if (classTyRes instanceof Ty classTy) {
                    this.expressionTypes.set(newObject, classTy);
                }
            }
        }

    }

    private Optional<IntrinsicMethod> isIntrinsicMethodCall(MethodCallExpression methodCall) {
        var maybeTarget = methodCall.getTarget();

        var methodName = methodCall.getIdentifier().getContent();

        var expectedField = switch (methodName) {
            case "println", "write", "flush" -> "out";
            case "read" -> "in";
            default -> null;
        };

        if (expectedField == null) {
            return Optional.empty();
        }

        if (maybeTarget.isPresent()) {
            var target = maybeTarget.get();

            if (target instanceof FieldAccessExpression fa && fa.getIdentifier().getContent().equals(expectedField)) {
                var fieldTarget = fa.getTarget();

                if (fieldTarget instanceof Reference r && r.getIdentifier().getContent().equals("System")) {
                    var systemSymbol = this.symbols.lookupDefinition("System");

                    var systemClass = Optional.ofNullable(this.classInfo.get("System"));

                    if (systemSymbol.isEmpty() && systemClass.isEmpty()) {
                        return Optional.of(switch (methodName) {
                            case "println" -> IntrinsicMethod.SYSTEM_OUT_PRINTLN;
                            case "write" -> IntrinsicMethod.SYSTEM_OUT_WRITE;
                            case "flush" -> IntrinsicMethod.SYSTEM_OUT_FLUSH;
                            case "read" -> IntrinsicMethod.SYSTEM_IN_READ;
                            default -> throw new AssertionError("Unreacheable");
                        });
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }

        return Optional.empty();
    }

    private void resolveType(Type ty) {
        switch (ty) {
            case ArrayType arrayTy -> {
                resolveType(arrayTy.getChildType());
            }
            case IntType intTy -> {
            }
            case BoolType boolTy -> {
            }
            case VoidType voidTy -> {
            }
            case ClassType classType -> {
                var ident = classType.getIdentifier();

                resolveClassType(classType);
            }
        }
    }

    private void resolveClassType(ClassType type) {
        var klass = Optional.ofNullable(this.classInfo.get(type.getIdentifier().getContent()));

        if (klass.isPresent()) {
            this.definitions.setClass(type, klass.get().definition);
        } else {
            reportError(new ClassDoesNotExist(type));
        }
    }

    private Optional<ClassTy> identToClassTy(String ident) {
        return Optional.ofNullable(this.classInfo.get(ident)).map(info -> info.type);
    }

    private ClassTy getCurrentClassTy() {
        return identToClassTy(this.currentClass.getIdentifier().getContent()).get();
    }

    private TyResult fromAstType(Type astType) {
        switch (astType) {
            case IntType ignored -> {
                return new IntTy();
            }
            case BoolType ignored -> {
                return new BoolTy();
            }
            case ClassType c -> {
                var klass = Optional.ofNullable(this.classInfo.get(c.getIdentifier().getContent()));

                if (klass.isPresent()) {
                    return klass.get().type;
                } else {
                    return new UnresolveableTy();
                }
            }
            case ArrayType a -> {
                var childType = this.fromAstType(a.getChildType());

                if (childType instanceof Ty childTy) {
                    return new ArrayTy(childTy, 1);
                } else {
                    return childType;
                }
            }
            case VoidType ignored -> {
                return new VoidTy();
            }
        }
        throw new AssertionError("Unreacheable, the above is exhaustive");
    }
}
