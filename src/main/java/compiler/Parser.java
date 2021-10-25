package compiler;

import compiler.ast.Class;
import compiler.ast.*;

import java.util.ArrayList;
import java.util.Optional;

import static compiler.Grammar.NonT;

public class Parser {

    //private final Lexer lexer;
    private final MockLexer lexer;
    private Token token;

    public Parser(MockLexer lexer) {
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

    Expression parseExpression2(int minPrec) {
        var result = this.parseUnaryExpression();

        while (getBinOpPrecedence(token.type) >= minPrec) {
            var tokenPrec = getBinOpPrecedence(token.type) + 1;
            this.token = lexer.nextToken();
            var rhs = this.parseExpression2(tokenPrec);
            result = constructBinOpExpression(result, this.token.type, rhs);
        }

        return result;
    }

    private static Expression constructBinOpExpression(Expression lhs, TokenType token, Expression rhs) {
        if (token == TokenType.Assign) {
            return new AssignmentExpression(lhs, rhs);
        }
        var op = switch (token) {
            case Or -> BinaryOpExpression.BinaryOp.Or;
            case And -> BinaryOpExpression.BinaryOp.And;
            case Equals -> BinaryOpExpression.BinaryOp.Equal;
            case NotEquals -> BinaryOpExpression.BinaryOp.NotEqual;
            case GreaterThan -> BinaryOpExpression.BinaryOp.Greater;
            case GreaterThanOrEquals -> BinaryOpExpression.BinaryOp.GreaterEqual;
            case LessThan -> BinaryOpExpression.BinaryOp.Less;
            case LessThanOrEquals -> BinaryOpExpression.BinaryOp.LessEqual;
            case Add -> BinaryOpExpression.BinaryOp.Addition;
            case Subtract -> BinaryOpExpression.BinaryOp.Subtraction;
            case Multiply -> BinaryOpExpression.BinaryOp.Multiplication;
            case Divide -> BinaryOpExpression.BinaryOp.Division;
            case Modulo -> BinaryOpExpression.BinaryOp.Modulo;
            default -> throw new AssertionError("Only call this function with binary op tokens.");
        };

        return new BinaryOpExpression(lhs, op, rhs);
    }

    private static int getBinOpPrecedence(TokenType type) {
        return switch (type) {
            case Assign -> 10;
            case Or -> 20;
            case And -> 30;
            case Equals, NotEquals -> 40;
            case GreaterThan, GreaterThanOrEquals, LessThan, LessThanOrEquals -> 50;
            case Add, Subtract -> 60;
            case Multiply, Divide, Modulo -> 70;
            default -> -1;
        };
    }

    private Expression parseAssignmentExpression() {
        Expression lftExpression = parseOrExpression();
        Optional<Expression> rghtExpression = Optional.empty();
        if (token.type == TokenType.Assign) {
            expect(TokenType.Assign);
            rghtExpression = Optional.of(parseAssignmentExpression());
        }
        //return new AssignmentExpression(lftExpression, rghtExpression);
        return null;
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
        }
        if (token.type == TokenType.Subtract) {
            expect(TokenType.Subtract);
            return parseUnaryExpression();
        }
        throw new Error();
    }

    private Expression parsePostfixExpression() {
        Expression expression = parsePrimaryExpression();
        while (true) {
            if (token.type == TokenType.Dot) {
                expect(TokenType.Dot);
                var ident = token.getIdentContent();
                expect(TokenType.Identifier);
                if (token.type == TokenType.LeftParen) {
                    expect(TokenType.LeftParen);
                    var arguments = parseArguments();
                    expect(TokenType.RightParen);
                    expression = new MethodCallExpression(Optional.of(expression), ident, arguments);
                } else {
                    expression = new FieldAccessExpression(expression, ident);
                }
            } else if (token.type == TokenType.LeftSquareBracket) {
                expect(TokenType.LeftSquareBracket);
                var inner = parseExpression();
                expect(TokenType.RightSquareBracket);
                expression = new ArrayAccessExpression(expression, inner);
            } else {
                return expression;
            }
        }
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

    private Expression parsePrimaryExpression() {
        if (token.type == TokenType.Null) {
            expect(TokenType.Null);
            return new NullExpression();
        } else if (token.type == TokenType.False) {
            expect(TokenType.False);
            return new BoolLiteral(false);
        } else if (token.type == TokenType.True) {
            expect(TokenType.True);
            return new BoolLiteral(true);
        } else if (token.type == TokenType.IntLiteral) {
            int value = token.getIntLiteralContent();
            expect(TokenType.IntLiteral);
            return new IntLiteral(value);
        } else if (token.type == TokenType.Identifier) {
            String ident = token.getIdentContent();
            expect(TokenType.Identifier);
            if (token.type == TokenType.LeftParen) {
                expect(TokenType.LeftParen);
                var arguments = parseArguments();
                expect(TokenType.RightParen);
                return new MethodCallExpression(Optional.empty(), ident, arguments);
            }
            return new Reference(ident);
        } else if (token.type == TokenType.This) {
            expect(TokenType.This);
            return new ThisExpression();
        } else if (token.type == TokenType.LeftParen) {
            expect(TokenType.LeftParen);
            var expression = parseExpression();
            expect(TokenType.RightParen);
            return expression;
        } else if (token.type == TokenType.New) {
            expect(TokenType.New);
            Token nextToken = this.lexer.peekToken();
            if (nextToken.type == TokenType.LeftParen) {
                return parseNewObjectExpression();
            } else if (nextToken.type == TokenType.LeftSquareBracket) {
                return parseNewArrayExpression();
            } else {
                throw new Error();
            }
        } else {
            throw new Error();
        }
    }

    private Expression parseNewObjectExpression() {
        var identifier = this.token.getIdentContent();
        expect(TokenType.Identifier);
        expect(TokenType.LeftParen);
        expect(TokenType.RightParen);
        return new NewObjectExpression(identifier);
    }

    private Expression parseNewArrayExpression() {
        int dimensions = 1;
        Type type = parseBasicType();
        expect(TokenType.LeftSquareBracket);
        Expression expression = parseExpression();
        expect(TokenType.RightSquareBracket);
        while (token.type == TokenType.LeftSquareBracket) {
            expect(TokenType.LeftSquareBracket);
            expect(TokenType.RightSquareBracket);
            dimensions++;
        }
        return new NewArrayExpression(type, expression, dimensions);
    }

}
