package compiler;

import compiler.ast.*;
import compiler.ast.Class;
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

    public NameResolution() {
        this.symbols = null;
        this.definitions = new AstData<>();
        this.types = new AstData<>();
        this.classEnvironments = null;
        this.currentClass = null;
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

    public static NameResolutionResult performNameResolution(Program program) {
        var resolution = new NameResolution();

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
                    if (targetType.isPresent() && targetType.get() instanceof ClassTy classTy) {

                        var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

                        var methodDef = classEnv.methods.get(methodCall.getIdentifier());
                        if (methodDef == null) {
                            // TODO: emit error
                        }

                        this.definitions.set(methodCall, methodDef);

                        var returnType = methodDef.getReturnType();

                        if (!(returnType instanceof VoidType)) {
                            this.types.set(methodCall, Ty.fromAstType(returnType, this.definitions));
                        }

                    } else {
                        // TODO: emit error
                    }
                }

                for (var arg : methodCall.getArguments()) {
                    resolveExpression(arg);
                }
            }
            case FieldAccessExpression fieldAccess -> {
                var target = fieldAccess.getTarget();
                resolveExpression(target);

                var targetType = this.types.get(target);
                if (targetType.isPresent() && targetType.get() instanceof ClassTy classTy) {

                    var classEnv = this.classEnvironments.get(classTy.getDefinition()).get();

                    var fieldDef = classEnv.fields.get(fieldAccess.getIdentifier());
                    if (fieldDef == null) {
                        // TODO: emit error
                    }

                    this.definitions.set(fieldAccess, fieldDef);

                    var type = fieldDef.getType();

                    if (!(type instanceof VoidType)) {
                        this.types.set(fieldAccess, Ty.fromAstType(type, this.definitions));
                    }

                } else {
                    // TODO: emit error
                }
            }
            case ArrayAccessExpression arrayAccess -> {
                resolveExpression(arrayAccess.getTarget());

                var targetType = this.types.get(arrayAccess.getTarget());

                if (targetType.isPresent()) {
                    if (targetType.get() instanceof ArrayTy arrayTy) {
                        this.types.set(arrayAccess, arrayTy.getChildTy());
                    } else {
                        // TODO: emit error
                    }
                } else {
                    // TODO: emit error
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
                    // TODO: report error;
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

                var klass = this.classes.get(ident);

                if (klass == null) {
                    // TODO: emit error
                }

                this.types.set(newObject, new ClassTy(klass));
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
            // TODO: report resolution error
        }
    }
}
