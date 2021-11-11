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
import java.util.function.Consumer;
import java.util.function.Function;

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

    private final AstData<AstNode> definitions; // Maps every ast node with an identifier to the ast node that defines it.
    private final AstData<TyResult> types; // Types of all expressions

    // Class -> Fields and Methods
    private AstData<ClassEnvironment> classEnvironments;

    private record ClassInfo(Class klass, ClassTy type) {
    }

    // Identifier -> (Class, ClassTy)
    private Map<String, ClassInfo> classInfo;

    private AstData<TyResult> memberTypes; // Field type or Method return type.

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

        this.definitions = new AstData<>();
        this.types = new AstData<>();

        this.classEnvironments = null;

        this.classInfo = null;
        this.memberTypes = new AstData<>();

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
        this.memberTypes = new AstData<>();

        for (Class klass : program.getClasses()) {
            var env = new ClassEnvironment();

            for (Method m : klass.getMethods()) {
                var returnType = m.getReturnType();
                this.resolveType(returnType);
                var returnTy = this.fromAstType(returnType);

                this.memberTypes.set(m, returnTy);
                env.addMethod(m);

                if (!(returnTy instanceof Ty) && !(returnTy instanceof VoidTy)) {
                    reportError(new UnresolveableMemberType(m));
                }

                for (Parameter param : m.getParameters()) {
                    this.resolveType(param.getType());
                }
            }

            for (Field f : klass.getFields()) {
                var type = f.getType();
                this.resolveType(type);
                var ty = this.fromAstType(type);

                this.memberTypes.set(f, ty);
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
                resolution.types
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
            }
            case ExpressionStatement exprStmt -> {
                resolveExpression(exprStmt.getExpression());
            }
            case WhileStatement whileStmt -> {
                resolveExpression(whileStmt.getCondition());
                resolveStatement(whileStmt.getBody());
            }
            case ReturnStatement retStmt -> {
                retStmt.getExpression().ifPresent(this::resolveExpression);
            }
            case LocalVariableDeclarationStatement declStmt -> {
                resolveType(declStmt.getType());

                symbols.insert(declStmt.getIdentifier(), declStmt);

                declStmt.getInitializer().ifPresent(this::resolveExpression);
            }
        }
    }

    private void resolveMethodCallWithTarget(MethodCallExpression methodCall, TyResult targetType) {
        if (targetType instanceof ClassTy classTy) {

            var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

            var methodDef = classEnv.searchMethod(methodCall.getIdentifier());
            if (methodDef.isPresent()) {
                this.definitions.set(methodCall, methodDef.get());

                var returnType = this.memberTypes.get(methodDef.get()).get();

                this.types.set(methodCall, returnType);
            } else {
                reportError(new UnresolveableMemberAccess(classTy.getDefinition(), methodCall));
                this.types.set(methodCall, new UnresolveableTy());
            }
        } else {
            reportError(new MemberAccessOnNonClassType(methodCall, targetType));
            this.types.set(methodCall, new UnresolveableTy());
        }
    }


    private void resolveExpression(Expression expression) {
        switch (expression) {
            case AssignmentExpression assign -> {
                var lval = assign.getLvalue();
                var rval = assign.getRvalue();

                resolveExpression(lval);
                resolveExpression(rval);

                this.types.set(assign, new UnresolveableTy());
            }
            case BinaryOpExpression binaryOp -> {
                resolveExpression(binaryOp.getLhs());
                resolveExpression(binaryOp.getRhs());

                this.types.set(binaryOp, new UnresolveableTy());
            }
            case UnaryExpression unaryOp -> {
                resolveExpression(unaryOp.getExpression());

                this.types.set(unaryOp, new UnresolveableTy());
            }
            case MethodCallExpression methodCall -> {
                var target = methodCall.getTarget();

                if (target.isPresent()) {
                    resolveExpression(target.get());

                    var targetType = this.types.get(target.get()).get();

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
            }
            case FieldAccessExpression fieldAccess -> {
                var target = fieldAccess.getTarget();
                resolveExpression(target);

                var targetType = this.types.get(target).get();

                // If the target expression doesn't have a type an error was reported during resolve of this expression.
                if (targetType instanceof ClassTy classTy) {

                    var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

                    var fieldDef = classEnv.searchField(fieldAccess.getIdentifier());
                    if (fieldDef.isPresent()) {

                        this.definitions.set(fieldAccess, fieldDef.get());

                        var ty = this.memberTypes.get(fieldDef.get());

                        ty.ifPresent(value -> this.types.set(fieldAccess, value));
                    } else {
                        reportError(new UnresolveableMemberAccess(classTy.getDefinition(), fieldAccess));
                        this.types.set(fieldAccess, new UnresolveableTy());
                    }
                } else {
                    reportError(new MemberAccessOnNonClassType(fieldAccess, targetType));
                    this.types.set(fieldAccess, new UnresolveableTy());
                }
            }
            case ArrayAccessExpression arrayAccess -> {
                resolveExpression(arrayAccess.getTarget());

                var targetType = this.types.get(arrayAccess.getTarget()).get();

                if (targetType instanceof ArrayTy arrayTy) {
                    this.types.set(arrayAccess, arrayTy.getChildTy());
                } else {
                    reportError(new ArrayAccessOnNonArrayType(arrayAccess, targetType));
                    this.types.set(arrayAccess, new UnresolveableTy());
                }

                resolveExpression(arrayAccess.getIndexExpression());
            }
            case NewArrayExpression newArray -> {
                resolveType(newArray.getType());

                var childType = this.fromAstType(newArray.getType());
                switch (childType) {
                    case Ty childTy -> {
                        this.types.set(newArray, new ArrayTy(childTy, newArray.getDimensions()));
                    }
                    default -> {
                        reportError(new IllegalNewArrayExpression(childType, newArray));
                        this.types.set(newArray, new UnresolveableTy());
                    }
                }

                resolveExpression(newArray.getFirstDimensionSize());
            }
            case Reference ref -> {
                var def = this.symbols.lookupDefinition(ref.getIdentifier());
                if (def.isPresent()) {
                    this.definitions.set(ref, (AstNode) def.get());

                    var type = def.get().getType();

                    var ty = this.fromAstType(type);
                    // If the type is not valid, an error was emitted when resolving the definition.
                    this.types.set(ref, ty);
                } else {
                    reportError(new UnresolveableReference(ref));
                    this.types.set(ref, new UnresolveableTy());
                }
            }
            case NullExpression nullExpr -> {
                this.types.set(nullExpr, new UnresolveableTy());
            }
            case BoolLiteral boolLit -> {
                this.types.set(boolLit, new UnresolveableTy());
            }
            case IntLiteral intLit -> {
                this.types.set(intLit, new UnresolveableTy());
            }
            case ThisExpression thisExpr -> {
                this.definitions.set(thisExpr, this.currentClass);
                this.types.set(thisExpr, this.getCurrentClassTy());
            }
            case NewObjectExpression newObject -> {
                var ident = newObject.getTypeIdentifier();
                resolveTypeIdent(ident, newObject);

                var classTy = identToClassTy(ident);

                // If not present, resolveTypeIdent will report an error
                classTy.ifPresent(ty -> this.types.set(newObject, ty));
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

    public TyResult fromAstType(Type astType) {
        switch (astType) {
            case IntType i -> {
                // TODO: do I really want to create a new object everytime...
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
                    return new ArrayTy(childTy, 0);
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
