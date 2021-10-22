package compiler;

import compiler.ast.*;
import compiler.ast.Class;

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
        return null;
    }

}
