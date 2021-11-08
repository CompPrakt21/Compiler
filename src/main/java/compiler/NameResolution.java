package compiler;

import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.types.ArrayTy;
import compiler.types.ClassTy;
import compiler.types.Ty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private final AstData<AstNode> definitions;
    private final AstData<Ty> types;

    // Class -> Fields and Methods
    private AstData<ClassEnvironment> classEnvironments;

    // Identifier -> Class
    private Map<String, Class> classes;

    private Optional<CompilerMessageReporter> reporter;

    public NameResolution() {
        this(Optional.empty());
    }

    public NameResolution(CompilerMessageReporter reporter) {
        this(Optional.of(reporter));
    }

    private NameResolution(Optional<CompilerMessageReporter> reporter) {
        this.symbols = null;
        this.definitions = new AstData<>();
        this.types = new AstData<>();
        this.classEnvironments = null;
        this.currentClass = null;
        this.reporter = reporter;
    }

    private void reportError(CompilerMessage msg) {
        if (this.reporter.isPresent()) {
            this.reporter.get().reportMessage(msg);
        }
    }

    public record NameResolutionResult(AstData<AstNode> definitions,
                                       AstData<Ty> types) {
    }

    private static AstData<ClassEnvironment> collectClassEnvironments(Program program) {
        AstData<ClassEnvironment> data = new AstData<>();

        for (Class klass : program.getClasses()) {
            var env = new ClassEnvironment();

            for (Method m : klass.getMethods()) {
                env.addMethod(m);
            }

            for (Field f : klass.getFields()) {
                env.addField(f);
            }
            data.set(klass, env);
        }

        return data;
    }

    private static Map<String, Class> collectClasses(Program program) {
        var map = new HashMap<String, Class>();

        for (Class klass : program.getClasses()) {
            map.put(klass.getIdentifier(), klass);
        }

        return map;
    }


    public static NameResolutionResult performNameResolution(Program program, CompilerMessageReporter reporter) {
        var resolution = new NameResolution(reporter);

        resolution.classEnvironments = collectClassEnvironments(program);
        resolution.classes = collectClasses(program);

        for (Class klass : program.getClasses()) {
            resolution.currentClass = klass;

            for (Field f : klass.getFields()) {
                resolution.resolveField(f);
            }

            for (Method m : klass.getMethods()) {
                resolution.resolveMethod(m);
            }
        }

        return new NameResolutionResult(
                resolution.definitions,
                resolution.types
        );
    }

    private void resolveField(Field field) {
        resolveType(field.getType());
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

    private void resolveMethodCallWithTarget(MethodCallExpression methodCall, Ty targetType) {
        if (targetType instanceof ClassTy classTy) {

            var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

            var methodDef = classEnv.methods.get(methodCall.getIdentifier());
            if (methodDef != null) {
                this.definitions.set(methodCall, methodDef);

                var returnType = methodDef.getReturnType();

                if (!(returnType instanceof VoidType)) {
                    this.types.set(methodCall, Ty.fromAstType(returnType, this.definitions));
                }
            } else {
                reportError(new UnresolveableMemberAccess(classTy.getDefinition(), methodCall));
            }
        } else {
            reportError(new MemberAccessOnNonClassType(methodCall, targetType));
        }
    }


    private void resolveExpression(Expression expression) {
        switch (expression) {
            case AssignmentExpression assign -> {
                var lval = assign.getLvalue();
                var rval = assign.getRvalue();

                resolveExpression(lval);
                resolveExpression(rval);
            }
            case BinaryOpExpression binaryOp -> {
                resolveExpression(binaryOp.getLhs());
                resolveExpression(binaryOp.getRhs());
            }
            case UnaryExpression unaryOp -> {
                resolveExpression(unaryOp.getExpression());
            }
            case MethodCallExpression methodCall -> {
                var target = methodCall.getTarget();

                if (target.isPresent()) {
                    resolveExpression(target.get());

                    var targetType = this.types.get(target.get());

                    // If the target expression doesn't have a type an error was reported during resolve of this expression.
                    if (targetType.isPresent()) {
                        resolveMethodCallWithTarget(methodCall, targetType.get());
                    }
                } else {
                    // Method call without target. => Target is this.
                    var targetType = new ClassTy(this.currentClass);
                    resolveMethodCallWithTarget(methodCall, targetType);
                }

                for (var arg : methodCall.getArguments()) {
                    resolveExpression(arg);
                }
            }
            case FieldAccessExpression fieldAccess -> {
                var target = fieldAccess.getTarget();
                resolveExpression(target);

                var targetType = this.types.get(target);

                // If the target expression doesn't have a type an error was reported during resolve of this expression.
                if (targetType.isPresent()) {
                    if (targetType.get() instanceof ClassTy classTy) {

                        var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

                        var fieldDef = classEnv.fields.get(fieldAccess.getIdentifier());
                        if (fieldDef == null) {
                            reportError(new UnresolveableMemberAccess(classTy.getDefinition(), fieldAccess));
                        }

                        this.definitions.set(fieldAccess, fieldDef);

                        var type = fieldDef.getType();

                        if (!(type instanceof VoidType)) {
                            this.types.set(fieldAccess, Ty.fromAstType(type, this.definitions));
                        }

                    } else {
                        reportError(new MemberAccessOnNonClassType(fieldAccess, targetType.get()));
                    }
                }
            }
            case ArrayAccessExpression arrayAccess -> {
                resolveExpression(arrayAccess.getTarget());

                var targetType = this.types.get(arrayAccess.getTarget());

                if (targetType.isPresent()) {
                    if (targetType.get() instanceof ArrayTy arrayTy) {
                        this.types.set(arrayAccess, arrayTy.getChildTy());
                    } else {
                        reportError(new ArrayAccessOnNonArrayType(arrayAccess, targetType.get()));
                    }
                } else {
                    reportError(new ArrayAccessOnNonArrayType(arrayAccess));
                }

                resolveExpression(arrayAccess.getIndexExpression());
            }
            case NewArrayExpression newArray -> {
                resolveType(newArray.getType());

                var childType = Ty.fromAstType(newArray.getType(), this.definitions);
                this.types.set(newArray, new ArrayTy(childType, newArray.getDimensions()));

                resolveExpression(newArray.getFirstDimensionSize());
            }
            case Reference ref -> {
                var def = this.symbols.lookupDefinition(ref.getIdentifier());
                if (def.isPresent()) {
                    this.definitions.set(ref, (AstNode) def.get());

                    var type = def.get().getType();

                    var ty = Ty.fromAstType(type, this.definitions);

                    this.types.set(ref, ty);
                } else {
                    reportError(new UnresolveableReference(ref));
                }
            }
            case NullExpression nullExpr -> {
            }
            case BoolLiteral boolLit -> {
            }
            case IntLiteral intLit -> {
            }
            case ThisExpression thisExpr -> {
                this.definitions.set(thisExpr, this.currentClass);
                this.types.set(thisExpr, new ClassTy(this.currentClass));
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
        var klass = Optional.ofNullable(this.classes.get(ident));

        if (klass.isPresent()) {
            this.definitions.set(ast, klass.get());
        } else {
            reportError(new ClassDoesNotExist(ident, ast.getSpan()));
        }
    }

    private Optional<ClassTy> identToClassTy(String ident) {
        var klass = Optional.ofNullable(this.classes.get(ident));

        return klass.map(ClassTy::new);
    }
}
