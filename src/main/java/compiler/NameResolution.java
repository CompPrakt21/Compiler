package compiler;

import compiler.ast.*;
import compiler.ast.Class;

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

    private final AstData<VariableDefinition> variableDefinitions;
    private final AstData<Class> typeDefinitions;

    private AstData<ClassEnvironment> classEnvironments;
    private Map<String, Class> classes;

    private NameResolution() {
        this.symbols = null;
        this.variableDefinitions = new AstData<>();
        this.typeDefinitions = new AstData<>();
        this.classEnvironments = null;
        this.currentClass = null;
    }

    public record NameResolutionResult(AstData<VariableDefinition> variableDefinitions,
                                       AstData<Class> typeDefinitions) {
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

        return new NameResolutionResult(resolution.variableDefinitions, resolution.typeDefinitions);
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
                resolveExpression(assign.getLvalue());
                resolveExpression(assign.getRvalue());
            }
            case BinaryOpExpression binaryOp -> {
                resolveExpression(binaryOp.getLhs());
                resolveExpression(binaryOp.getRhs());
            }
            case UnaryExpression unaryOp -> {
                resolveExpression(unaryOp.getExpression());
            }
            case MethodCallExpression methodCall -> {

                methodCall.getTarget().ifPresent(this::resolveExpression);

                for (var arg : methodCall.getArguments()) {
                    resolveExpression(arg);
                }

            }
            case FieldAccessExpression fieldAccess -> {
                resolveExpression(fieldAccess.getTarget());
            }
            case ArrayAccessExpression arrayAccess -> {
                resolveExpression(arrayAccess.getTarget());
                resolveExpression(arrayAccess.getIndexExpression());
            }
            case NewArrayExpression newArray -> {
                resolveType(newArray.getType());
                resolveExpression(newArray.getFirstDimensionSize());
            }
            case Reference ref -> {
                var def = this.symbols.lookupDefinition(ref.getIdentifier());
                if (def.isPresent()) {
                    this.variableDefinitions.set(ref, def.get());
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
                this.typeDefinitions.set(thisExpr, this.currentClass);
            }
            case NewObjectExpression newObject -> {
                var ident = newObject.getTypeIdentifier();
                resolveTypeIdent(ident, newObject);
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
            this.typeDefinitions.set(ast, klass.get());
        } else {
            // TODO: report resolution error
        }
    }
}
