package compiler;

import compiler.ast.Class;
import compiler.ast.*;

import java.util.ArrayList;
import java.util.Optional;

import static compiler.Grammar.NonT;

public class Parser {

    private final Lexer lexer;
    private Token token;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.token = lexer.nextToken();
    }

    private void expect(TokenType type) {
        if (this.token.type != type) {
            throw new Error();
        }
        this.token = lexer.nextToken();
    }

    private String expectIdent() {
        if (this.token.type != TokenType.Identifier) {
            throw new Error();
        }
        String name = this.token.getIdentContent();
        this.token = lexer.nextToken();
        return name;
    }

    public AstNode parse() {
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
        var identifier = expectIdent();
        expect(TokenType.LeftCurlyBracket);
        while (NonT.ClassMember.firstContains(token.type)) {
            var ast = parseClassMember();
            if (ast instanceof Field) {
                fields.add((Field) ast);
            } else {
                assert ast instanceof Method;
                methods.add((Method) ast);
            }
        }
        expect(TokenType.RightCurlyBracket);

        return new Class(identifier, fields, methods);
    }

    private AstNode parseClassMember() {
        // This parses methods until the left paren, including
        boolean isStatic = false;
        expect(TokenType.Public);
        if (token.type == TokenType.Static) {
            expect(TokenType.Static);
            isStatic = true;
        }
        var type = parseType();
        var ident = expectIdent();
        if (token.type == TokenType.SemiColon) {
            expect(TokenType.SemiColon);
            return new Field(ident, type);
        } else if (token.type == TokenType.LeftParen) {
            expect(TokenType.LeftParen);
            return parseMethod(type, ident, isStatic);
        } else {
            throw new Error();
        }
    }

    private Method parseMethod(Type returnType, String name, boolean isStatic) {
        var parameters = new ArrayList<Parameter>();

        if (isStatic) {
            // Handling MainMethod, parse up to the closing paren,
            // rest will be parsed together
            var parameter = parseParameter();
            parameters.add(parameter);
        } else {
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
        return new Method(isStatic, name, returnType, parameters, body);
    }

    private Parameter parseParameter() {
        var type = parseType();
        var identifier = expectIdent();
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
                expect(TokenType.Int);
                return new IntType();
            }
            case Boolean -> {
                expect(TokenType.Boolean);
                return new BoolType();
            }
            case Void -> {
                expect(TokenType.Void);
                return new VoidType();
            }
            case Identifier -> {
                return new ClassType(expectIdent());
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
        } else if (NonT.LocalVariableDeclarationStatement.firstContains(token.type)) {
            return parseLocalVariableDeclarationStatement();
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
        var condition = parseExpression(0);
        expect(TokenType.RightParen);
        var thenStatement = parseStatement();
        Optional<Statement> elseStatement = Optional.empty();
        if (token.type == TokenType.Else) {
            expect(TokenType.Else);
            elseStatement = Optional.of(parseStatement());
        }
        return new IfStatement(condition, thenStatement, elseStatement);
    }

    private ExpressionStatement parseExpressionStatement() {
        var expression = parseExpression(0);
        expect(TokenType.SemiColon);
        return new ExpressionStatement(expression);
    }

    private WhileStatement parseWhileStatement() {
        expect(TokenType.While);
        expect(TokenType.LeftParen);
        var condition = parseExpression(0);
        expect(TokenType.RightParen);
        var body = parseStatement();
        return new WhileStatement(condition, body);
    }

    private ReturnStatement parseReturnStatement() {
        expect(TokenType.Return);
        Optional<Expression> expression = Optional.empty();
        if (NonT.Expression.firstContains(token.type)) {
            expression = Optional.of(parseExpression(0));
        }
        expect(TokenType.SemiColon);
        return new ReturnStatement(expression);
    }

    private LocalVariableDeclarationStatement parseLocalVariableDeclarationStatement() {
        var type = parseType();
        var ident = expectIdent();
        Optional<Expression> initializer = Optional.empty();
        if (token.type == TokenType.Assign) {
            expect(TokenType.Assign);
            initializer = Optional.of(parseExpression(0));
        }
        return new LocalVariableDeclarationStatement(type, ident, initializer);
    }

    private Expression parseExpressionOld() {
        return parseAssignmentExpression();
    }

    Expression parseExpression(int minPrec) {
        var result = this.parseUnaryExpression();

        while (getBinOpPrecedence(token.type) >= minPrec) {
            var tokenPrec = getBinOpPrecedence(token.type) + 1;
            var oldToken = this.token;
            this.token = lexer.nextToken();
            var rhs = this.parseExpression(tokenPrec);
            result = constructBinOpExpression(result, oldToken.type, rhs);
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
        if (NonT.PostfixExpression.firstContains(token.type)) {
            return parsePostfixExpression();
        }
        if (token.type == TokenType.Not) {
            expect(TokenType.Not);
            return new UnaryExpression(parseUnaryExpression(), UnaryExpression.UnaryOp.LogicalNot);
        }
        if (token.type == TokenType.Subtract) {
            expect(TokenType.Subtract);
            return new UnaryExpression(parseUnaryExpression(), UnaryExpression.UnaryOp.Negate);
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
                var inner = parseExpression(0);
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
            arguments.add(parseExpression(0));
            while (token.type == TokenType.Comma) {
                expect(TokenType.Comma);
                arguments.add(parseExpression(0));
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
            var expression = parseExpression(0);
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
        var identifier = expectIdent();
        expect(TokenType.LeftParen);
        expect(TokenType.RightParen);
        return new NewObjectExpression(identifier);
    }

    private Expression parseNewArrayExpression() {
        int dimensions = 1;
        Type type = parseBasicType();
        expect(TokenType.LeftSquareBracket);
        Expression expression = parseExpression(0);
        expect(TokenType.RightSquareBracket);
        while (token.type == TokenType.LeftSquareBracket) {
            expect(TokenType.LeftSquareBracket);
            expect(TokenType.RightSquareBracket);
            dimensions++;
        }
        return new NewArrayExpression(type, expression, dimensions);
    }

}
