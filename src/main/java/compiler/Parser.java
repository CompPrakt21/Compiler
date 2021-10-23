package compiler;

import compiler.ast.*;
import compiler.ast.Class;
import picocli.CommandLine;

import javax.swing.text.html.Option;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static compiler.Grammar.*;

public class Parser {

    private Lexer lexer;
    private Token token;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
    }

    private void expect(TokenType type) {
        if (this.token.type != type) {
            throw new Error();
        }
        this.token = lexer.nextToken();
    }

    public AstNode parse() {
        this.token = lexer.nextToken();
        return parseS();
    }

    private AstNode parseS() {
        var ast = parseProgram();
        expect(TokenType.EOF);
        return ast;
    }

    private Program parseProgram() {
        var classes = new ArrayList<Class>();
        while (true) {
            if (NonT.ClassDeclaration.firstContains(token.type)) {
                var ast = parseClassDeclaration();
                classes.add(ast);
            } else if (NonT.Program.followContains(token.type)) {
                return new Program(classes);
            } else {
                throw new Error();
            }
        }
    }

    private Class parseClassDeclaration() {
        var fields = new ArrayList<Field>();
        var methods = new ArrayList<Method>();
        expect(TokenType.Class);
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        expect(TokenType.LeftCurlyBracket);
        if (NonT.ClassMember.firstContains(token.type)) {
            var ast = parseClassMember();
            if (ast instanceof Field) {
                fields.add((Field) ast);
            } else {
                assert ast instanceof Method;
                methods.add((Method) ast);
            }
        } else {
            expect(TokenType.RightCurlyBracket);
        }
        return new Class(identifier, fields, methods);
    }

    private AstNode parseClassMember() {
        if (NonT.Field.firstContains(token.type)) {
            return parseField();
        } else if (NonT.Method.firstContains(token.type)) {
            return parseMethod();
        } else {
            throw new Error();
        }
    }

    private Field parseField() {
        expect(TokenType.Public);
        var type = parseType();
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        expect(TokenType.SemiColon);
        return new Field(identifier, type);
    }

    private Method parseMethod() {
        boolean isStatic = false;
        Type methodType;
        String methodIdentifier;
        var parameters = new ArrayList<Parameter>();

        expect(TokenType.Public);
        if (token.type == TokenType.Static) {
            // Handling MainMethod, parse up to the closing paren,
            // rest will be parsed together
            isStatic = true;
            expect(TokenType.Static);
            expect(TokenType.Void);
            methodType = new VoidType();
            methodIdentifier = this.token.getIdentContent();
            expect(TokenType.Identifier);
            expect(TokenType.LeftParen);
            var parameter = parseParameter();
            parameters.add(parameter);
        } else {
            methodType = parseType();
            methodIdentifier = this.token.getIdentContent();
            expect(TokenType.Identifier);
            expect(TokenType.LeftParen);
            if (NonT.Parameter.firstContains(token.type)) {
                var parameter = parseParameter();
                parameters.add(parameter);
            }
            while (token.type == TokenType.Comma) {
                expect(TokenType.Comma);
                var parameter = parseParameter();
                parameters.add(parameter);
            }
        }
        expect(TokenType.RightParen);
        if (NonT.MethodRest.firstContains(token.type)) {
            expect(TokenType.Throws);
            expect(TokenType.Identifier);
        }
        var body = parseBlock();
        return new Method(isStatic, methodIdentifier, methodType, parameters, body);
    }

    private Parameter parseParameter() {
        var type = parseType();
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        return new Parameter(type, identifier);
    }

    private Type parseType() {
        var type = parseBasicType();
        while (token.type == TokenType.LeftSquareBracket) {
            expect(TokenType.LeftSquareBracket);
            expect(TokenType.RightSquareBracket);
            type = new ArrayType(type);
        }
        return type;
    }

    private Type parseBasicType() {
        switch (token.type) {
            case Int -> {
                return new IntType();
            }
            case Boolean -> {
                return new BoolType();
            }
            case Void -> {
                return new VoidType();
            }
            case Identifier -> {
                return new ClassType(token.getIdentContent());
            }
            default -> throw new Error();
        }
    }

    private Block parseBlock() {
        var statements = new ArrayList<Statement>();
        expect(TokenType.LeftCurlyBracket);
        while (NonT.BlockStatement.firstContains(token.type)) {
            var ast = parseStatement();
            statements.add(ast);
        }
        expect(TokenType.RightCurlyBracket);
        return new Block(statements);
    }

    private Statement parseStatement() {
        if (NonT.Block.firstContains(token.type)) {
            return parseBlock();
        } else if (NonT.EmptyStatement.firstContains(token.type)) {
            return parseEmptyStatement();
        } else if (NonT.IfStatement.firstContains(token.type)) {
            return parseIfStatement();
        } else if (NonT.ExpressionStatement.firstContains(token.type)) {
            return parseExpressionStatement();
        } else if (NonT.WhileStatement.firstContains(token.type)) {
            return parseWhileStatement();
        } else if (NonT.ReturnStatement.firstContains(token.type)) {
            return parseReturnStatement();
        } else {
            throw new Error();
        }
    }

    private EmptyStatement parseEmptyStatement() {
        expect(TokenType.SemiColon);
        return new EmptyStatement();
    }

    private IfStatement parseIfStatement() {
        expect(TokenType.If);
        expect(TokenType.LeftParen);
        var condition = parseExpression();
        var thenStatement = parseStatement();
        Optional<Statement> elseStatement = Optional.empty();
        if (token.type == TokenType.Else) {
            expect(TokenType.Else);
            elseStatement = Optional.of(parseStatement());
        }
        return new IfStatement(condition, thenStatement, elseStatement);
    }

    private ExpressionStatement parseExpressionStatement() {
        var expression = parseExpression();
        expect(TokenType.SemiColon);
        return new ExpressionStatement(expression);
    }

    private WhileStatement parseWhileStatement() {
        expect(TokenType.While);
        expect(TokenType.LeftParen);
        var condition = parseExpression();
        expect(TokenType.RightParen);
        var body = parseStatement();
        return new WhileStatement(condition, body);
    }

    private ReturnStatement parseReturnStatement() {
        expect(TokenType.Return);
        Optional<Expression> expression = Optional.empty();
        if (NonT.Expression.firstContains(token.type)) {
            expression = Optional.of(parseExpression());
        }
        expect(TokenType.SemiColon);
        return new ReturnStatement(expression);
    }

    private Expression parseExpression() {
        return parseAssignmentExpression();
    }

    private Expression parseAssignmentExpression() {
        Expression lftExpression = parseOrExpression();
        Optional<Expression> rghtExpression = Optional.empty();
        if (token.type == TokenType.Assign) {
            expect(TokenType.Assign);
            rghtExpression = Optional.of(parseAssignmentExpression());
        }
        return new AssignmentExpression(lftExpression, rghtExpression);
    }

    private Expression parseOrExpression() {
        Expression expression = parseAndExpression();
        while (token.type == TokenType.Or) {
            expect(TokenType.Or);
            expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Or, parseAndExpression());
        }
        return expression;
    }

    private Expression parseAndExpression() {
        Expression expression = parseEqualityExpression();
        while (token.type == TokenType.And) {
            expect(TokenType.And);
            expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.And, parseEqualityExpression());
        }
        return expression;
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseRelationalExpression();
        while (token.type == TokenType.Equals || token.type == TokenType.NotEquals) {
            if (token.type == TokenType.Equals) {
                expect(TokenType.Equals);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Equal, parseRelationalExpression());
            } else {
                expect(TokenType.NotEquals);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.NotEqual, parseRelationalExpression());
            }
        }
        return expression;
    }

    private Expression parseRelationalExpression() {
        Expression expression = parseAdditiveExpression();
        while (true) {
            if (token.type == TokenType.LessThan) {
                expect(TokenType.LessThan);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Less, parseAdditiveExpression());
            } else if (token.type == TokenType.LessThanOrEquals) {
                expect(TokenType.LessThanOrEquals);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.LessEqual, parseAdditiveExpression());
            } else if (token.type == TokenType.GreaterThan) {
                expect(TokenType.GreaterThan);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Greater, parseAdditiveExpression());
            } else if (token.type == TokenType.GreaterThanOrEquals) {
                expect(TokenType.GreaterThanOrEquals);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.GreaterEqual, parseAdditiveExpression());
            } else {
                return expression;
            }
        }
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseMultiplicativeExpression();
        while (true) {
            if (token.type == TokenType.Add) {
                expect(TokenType.Add);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Addition, parseMultiplicativeExpression());
            } else if (token.type == TokenType.Subtract) {
                expect(TokenType.Subtract);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Subtraction, parseMultiplicativeExpression());
            } else {
                return expression;
            }
        }
    }

    private Expression parseMultiplicativeExpression() {
        Expression expression = parseUnaryExpression();
        while (true) {
            if (token.type == TokenType.Multiply) {
                expect(TokenType.Multiply);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Multiplication, parseUnaryExpression());
            } else if (token.type == TokenType.Divide) {
                expect(TokenType.Divide);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Division, parseUnaryExpression());
            } else if (token.type == TokenType.Modulo) {
                expect(TokenType.Modulo);
                expression = new BinaryOpExpression(expression, BinaryOpExpression.BinaryOp.Modulo, parseUnaryExpression());
            } else {
                return expression;
            }
        }
    }

    private Expression parseUnaryExpression() {
        if (NonT.PostfixOp.firstContains(token.type)) {
            return parsePostfixExpression();
        }
        if (token.type == TokenType.Not) {
            expect(TokenType.Not);
            return parseUnaryExpression();
        }if (token.type == TokenType.Subtract) {
            expect(TokenType.Subtract);
            return parseUnaryExpression();
        }
    }

    private Expression parsePostfixExpression() {
        Expression expression = parsePrimaryExpression();

    }

    private Expression parsePostfixOperation() {
        if (NonT.MethodInvocation.firstContains(token.type)) {
            return parseMethodInvocation();
        } else if(NonT.FieldAccess.firstContains(token.type)) {
            return parseFieldAccess();
        } else if(NonT.ArrayAccess.firstContains(token.type)) {
            return parseArrayAccess();
        } else {
            throw new Error();
        }
    }

    private Expression parseMethodInvocation() {
        expect(TokenType.Dot);
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        expect(TokenType.LeftParen);
        ArrayList<Expression> arguments = parseArguments();
        expect(TokenType.RightParen);
        return new MethodCallExpression(identifier, arguments);

    }
    private Expression parseFieldAccess() {
        expect(TokenType.Dot);
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        return new FieldAccessExpression(identifier);
    }

    private Expression parseArrayAccess() {
        expect(TokenType.LeftSquareBracket);
        Expression expression = parseExpression();
        expect(TokenType.RightSquareBracket);
        return new ArrayAccessExpression(expression);
    }


    private ArrayList<Expression> parseArguments() {
        ArrayList<Expression> arguments = new ArrayList<>();
        if (NonT.Expression.firstContains(token.type)) {
            arguments.add(parseExpression());
            while (token.type == TokenType.Comma) {
                expect(TokenType.Comma);
                arguments.add(parseExpression());
            }
        }
        return arguments;
    }

    private Expression newObjectExpression() {
        expect(TokenType.New);
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        expect(TokenType.LeftParen);
        expect(TokenType.RightParen);
        return new NewObjectExpression(identifier);
    }

    private Expression newArrayExpression(){
        int dimensions = 1;
        expect(TokenType.New);
        Type type = parseBasicType();
        expect(TokenType.LeftSquareBracket);
        Expression expression = parseExpression();
        expect(TokenType.RightSquareBracket);
        while(token.type == TokenType.LeftSquareBracket) {
            expect(TokenType.LeftSquareBracket);
            expect(TokenType.RightSquareBracket);
            dimensions++;
        }
        return new NewArrayExpression(type, expression, dimensions);
    }

}
