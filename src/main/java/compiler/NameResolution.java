package compiler;

import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.types.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class ClassEnvironment {
    public Map<String, Method> methods;
    public Map<String, Field> fields;

    public ClassEnvironment() {
        this.methods = new HashMap<>();
        this.fields = new HashMap<>();
    }

    public void addMethod(Method m) {
        var ident = m.getIdentifier();
        this.methods.put(ident, m);
    }

    public void addField(Field f) {
        var ident = f.getIdentifier();
        this.fields.put(ident, f);
    }

    public Optional<Method> searchMethod(String ident) {
        return Optional.ofNullable(this.methods.get(ident));
    }

    public Optional<Field> searchField(String ident) {
        return Optional.ofNullable(this.fields.get(ident));
    }
}

public class NameResolution {

    private SymbolTable<VariableDefinition> symbols;
    private Class currentClass;
    private Method currentMethod;

    private final AstData<AstNode> definitions; // Maps every ast node with an identifier to the ast node that defines it.
    private final AstData<TyResult> expressionTypes; // Types of all expressions

    // Class -> Fields and Methods
    private AstData<ClassEnvironment> classEnvironments;

    private record ClassInfo(Class klass, ClassTy type) {
    }

    // Identifier -> (Class, ClassTy)
    private Map<String, ClassInfo> classInfo;

    private AstData<TyResult> bindingTypes; // Field type, Method return type, Parameter types or Local var declarations

    private Optional<CompilerMessageReporter> reporter;

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

        this.definitions = new AstData<>();
        this.expressionTypes = new AstData<>();

        this.classEnvironments = null;

        this.classInfo = null;
        this.bindingTypes = new AstData<>();

        this.reporter = reporter;
    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
    }

    public record NameResolutionResult(AstData<AstNode> definitions,
                                       AstData<TyResult> types) {
    }

    private void collectClasses(Program program) {
        this.classInfo = new HashMap<>();

        for (Class klass : program.getClasses()) {
            this.classInfo.put(klass.getIdentifier(), new ClassInfo(klass, new ClassTy(klass)));
        }
    }

    private void collectClassEnvironments(Program program) {
        this.classEnvironments = new AstData<>();
        this.bindingTypes = new AstData<>();

        for (Class klass : program.getClasses()) {
            var env = new ClassEnvironment();

            for (Method m : klass.getMethods()) {
                var returnType = m.getReturnType();
                this.resolveType(returnType);
                var returnTy = this.fromAstType(returnType);

                this.bindingTypes.set(m, returnTy);
                env.addMethod(m);

                if (!(returnTy instanceof Ty) && !(returnTy instanceof VoidTy)) {
                    reportError(new UnresolveableMemberType(m));
                }

                for (Parameter param : m.getParameters()) {
                    this.resolveType(param.getType());
                    var paramType = this.fromAstType(param.getType());

                    switch (paramType) {
                        case VoidTy ty -> {
                            reportError(new IllegalMethodParameterType(param, IllegalMethodParameterType.Reason.VOID));
                        }
                        case UnresolveableTy ty -> {
                            reportError(new IllegalMethodParameterType(param, IllegalMethodParameterType.Reason.UNRESOLVEABLE));
                        }
                        case Ty ty -> {
                        }
                    }

                    this.bindingTypes.set(param, paramType);
                }
            }

            for (Field f : klass.getFields()) {
                var type = f.getType();
                this.resolveType(type);
                var ty = this.fromAstType(type);

                this.bindingTypes.set(f, ty);
                env.addField(f);

                if (!(ty instanceof Ty)) {
                    reportError(new UnresolveableMemberType(f));
                }
            }

            this.classEnvironments.set(klass, env);
        }
    }

    public static NameResolutionResult performNameResolution(Program program, CompilerMessageReporter reporter) {
        var resolution = new NameResolution(reporter);

        resolution.collectClasses(program); // Collect global class information
        resolution.collectClassEnvironments(program); // Collect member (return) types and type check them. (valid class types, no void type)

        for (Class klass : program.getClasses()) {
            resolution.currentClass = klass;

            for (Method m : klass.getMethods()) {
                resolution.resolveMethod(m);
            }
        }

        return new NameResolutionResult(
                resolution.definitions,
                resolution.expressionTypes
        );
    }

    private void resolveMethod(Method method) {
        this.symbols = new SymbolTable<>();

        resolveType(method.getReturnType());

        for (Field f : this.currentClass.getFields()) {
            symbols.insert(f.getIdentifier(), f);
        }

        symbols.enterScope();

        for (var param : method.getParameters()) {
            symbols.insert(param.getIdentifier(), param);
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
            case ExpressionStatement exprStmt -> {
                resolveExpression(exprStmt.getExpression());
            }
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

                if (returnType instanceof Ty expectedReturnTy) {
                    if (retStmt.getExpression().isPresent()) {
                        var retExpr = retStmt.getExpression().get();
                        var retExprTyRes = this.expressionTypes.get(retExpr).get();

                        if (retExprTyRes instanceof Ty retExprTy) {
                            if (!(retExprTy.comparable(expectedReturnTy))) {
                                reportError(new ReturnStatementErrors.TypeMismatch(currentMethod, expectedReturnTy, retStmt, retExprTy));
                            }
                        }
                    } else {
                        reportError(new ReturnStatementErrors.MissingReturnExpr(this.currentMethod, retStmt, expectedReturnTy));
                    }
                } else {
                    assert returnType instanceof VoidTy;

                    if (retStmt.getExpression().isPresent()) {
                        reportError(new ReturnStatementErrors.UnexpectedReturnExpr(this.currentMethod, retStmt));
                    }
                }
            }
            case LocalVariableDeclarationStatement declStmt -> {
                resolveType(declStmt.getType());

                symbols.insert(declStmt.getIdentifier(), declStmt);

                declStmt.getInitializer().ifPresent(this::resolveExpression);

                var bindingTy = this.fromAstType(declStmt.getType());
                this.bindingTypes.set(declStmt, bindingTy);

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
                    case VoidTy ignored -> {
                        reportError(new LocalDeclarationErrors.VoidType(declStmt));
                    }
                    case UnresolveableTy ignored -> {
                        reportError(new LocalDeclarationErrors.UnresolveableType(declStmt));
                    }
                }
            }
        }
    }

    private void resolveMethodCallWithTarget(MethodCallExpression methodCall, TyResult targetType) {
        if (targetType instanceof ClassTy classTy) {

            var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

            var methodDef = classEnv.searchMethod(methodCall.getIdentifier());
            if (methodDef.isPresent()) {
                this.definitions.set(methodCall, methodDef.get());

                var returnType = this.bindingTypes.get(methodDef.get()).get();

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

    private void typeCheckSimpleBinaryExpression(BinaryOpExpression binaryOp, Ty expectedOperandTy, Ty operationTy, Ty lhsTy, Ty rhsTy) {
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
                case Addition, Subtraction, Multiplication, Division, Modulo -> {
                    typeCheckSimpleBinaryExpression(binaryOp, new IntTy(), new IntTy(), lhsTy, rhsTy);
                }
                case Greater, GreaterEqual, Less, LessEqual -> {
                    typeCheckSimpleBinaryExpression(binaryOp, new IntTy(), new BoolTy(), lhsTy, rhsTy);
                }
                case Equal, NotEqual -> {
                    if (!lhsTy.comparable(rhsTy)) {
                        reportError(new BinaryExpressionTypeMismatch.IncomparableTypes(binaryOp, lhsTy, rhsTy));
                    }
                    this.expressionTypes.set(binaryOp, new BoolTy());
                }
                case And, Or -> {
                    typeCheckSimpleBinaryExpression(binaryOp, new BoolTy(), new BoolTy(), lhsTy, rhsTy);
                }
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
                    if (lvalTy.equals(rvalTy)) {
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

                for (var arg : methodCall.getArguments()) {
                    resolveExpression(arg);
                }

                // Method arguments type checking
                var maybeMethod = this.definitions.get(methodCall);
                if (maybeMethod.isPresent()) {
                    assert maybeMethod.get() instanceof Method;
                    var methodDef = (Method) maybeMethod.get();

                    var argumentTypes = methodCall.getArguments().stream().map(arg -> this.expressionTypes.get(arg).get()).collect(Collectors.toList());
                    var paramTypes = methodDef.getParameters().stream().map(param -> this.bindingTypes.get(param).get()).collect(Collectors.toList());

                    if (argumentTypes.size() != paramTypes.size()) {
                        reportError(new MethodParameterErrors.DifferentLength(methodDef, methodCall));
                    }

                    for (int i = 0; i < Math.min(argumentTypes.size(), paramTypes.size()); i += 1) {
                        var argTyRes = argumentTypes.get(i);
                        var paramTyRes = paramTypes.get(i);

                        if (paramTyRes instanceof Ty paramTy) {
                            if (argTyRes instanceof VoidTy || argTyRes instanceof Ty argTy && !argTy.comparable(paramTy)) {
                                reportError(new MethodParameterErrors.ArgumentTypeMismatch(methodDef.getParameters().get(i), methodCall.getArguments().get(i), paramTy, argTyRes));
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

                    var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

                    var fieldDef = classEnv.searchField(fieldAccess.getIdentifier());
                    if (fieldDef.isPresent()) {

                        this.definitions.set(fieldAccess, fieldDef.get());

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
                var def = this.symbols.lookupDefinition(ref.getIdentifier());
                if (def.isPresent()) {
                    this.definitions.set(ref, (AstNode) def.get());

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
                this.definitions.set(thisExpr, this.currentClass);
                this.expressionTypes.set(thisExpr, this.getCurrentClassTy());
            }
            case NewObjectExpression newObject -> {
                var ident = newObject.getTypeIdentifier();
                resolveTypeIdent(ident, newObject);

                var classTy = identToClassTy(ident);

                // If not present, resolveTypeIdent will report an error
                classTy.ifPresent(ty -> this.expressionTypes.set(newObject, ty));
            }
        }

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
            case ClassType classTy -> {
                var ident = classTy.getIdentifier();

                resolveTypeIdent(ident, classTy);
            }
        }
    }

    private void resolveTypeIdent(String ident, AstNode ast) {
        var klass = Optional.ofNullable(this.classInfo.get(ident));

        if (klass.isPresent()) {
            this.definitions.set(ast, klass.get().klass);
        } else {
            reportError(new ClassDoesNotExist(ident, ast.getSpan()));
        }
    }

    private Optional<ClassTy> identToClassTy(String ident) {
        return Optional.ofNullable(this.classInfo.get(ident)).map(info -> info.type);
    }

    private ClassTy getCurrentClassTy() {
        return identToClassTy(this.currentClass.getIdentifier()).get();
    }

    private TyResult fromAstType(Type astType) {
        switch (astType) {
            case IntType i -> {
                return new IntTy();
            }
            case BoolType b -> {
                return new BoolTy();
            }
            case ClassType c -> {
                var klass = Optional.ofNullable(this.classInfo.get(c.getIdentifier()));

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
            case VoidType v -> {
                return new VoidTy();
            }
        }
        throw new AssertionError("Unreacheable, the above is exhaustive");
    }
}
