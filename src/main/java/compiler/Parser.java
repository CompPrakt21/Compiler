package compiler;

import compiler.ast.Class;
import compiler.ast.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static compiler.Grammar.NonT.*;
import static compiler.TokenType.*;

public class Parser {

    private final Lexer lexer;
    private Token token;
    private boolean errorMode;
    public boolean successfulParse;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.token = lexer.nextToken();
        this.errorMode = false;
        this.successfulParse = true;
    }

    private record ExpectResult(Token token, boolean isError) {
    }

    private ExpectResult expect(TokenSet anchors, TokenSetLike... type) {
        return this.expectInternal(anchors, true, type);
    }

    private Token assertExpect(TokenType type) {
        assert this.token.type == type;
        var oldToken = this.token;
        this.token = this.lexer.nextToken();
        return oldToken;
    }

    private ExpectResult expectNoConsume(TokenSet anchors, TokenSetLike... type) {
        return this.expectInternal(anchors, false, type);
    }

    private ExpectResult expectInternal(TokenSet anchors, boolean consume, TokenSetLike... type) {
        var expectedTokens = TokenSet.of(type);
        var error = false;

        if (!expectedTokens.contains(this.token.type)) {
            this.successfulParse = false;

            if (!this.errorMode) {
                // TODO: emit error;
            }
            this.errorMode = true;
            error = true;
            this.skipUntilAnchorSet(anchors.add(expectedTokens));
            if (!expectedTokens.contains(this.token.type)) {
                return new ExpectResult(null, error);
            }
        }

        var oldToken = this.token;
        if (consume) {
            this.token = lexer.nextToken();
        }
        this.errorMode = false;
        return new ExpectResult(oldToken, error);
    }

    private void skipUntilAnchorSet(TokenSet anchors) {
        while (!anchors.contains(this.token.type)) {
            this.token = this.lexer.nextToken();
        }
    }

    public Program parse() {
        return parseS(TokenSet.empty());
    }

    private Program parseS(TokenSet anchors) {
        var ast = parseProgram(anchors.add(EOF));
        expect(anchors, EOF);
        return ast;
    }

    private Program parseProgram(TokenSet anchors) {
        var classes = new ArrayList<Class>();
        boolean error = false;

        this.expectNoConsume(anchors, ClassDeclaration.first(), ClassDeclaration.follow());

        while (ClassDeclaration.firstContains(token.type)) {
            var ast = parseClassDeclaration(anchors);
            error |= ast == null;
            classes.add(ast);

            this.expectNoConsume(anchors, ClassDeclaration.first(), ClassDeclaration.follow());
        }

        return new Program(classes).makeError(error);
    }

    private Class parseClassDeclaration(TokenSet anchors) {
        var fields = new ArrayList<Field>();
        var methods = new ArrayList<Method>();

        var expectResult = expect(anchors.add(Identifier, LeftCurlyBracket, ClassDeclaration.first(), RightCurlyBracket), TokenType.Class);
        var error = expectResult.isError;

        expectResult = expect(anchors.add(LeftCurlyBracket, ClassDeclaration.first(), RightCurlyBracket), TokenType.Identifier);
        error |= expectResult.isError;
        var identifier = expectResult.isError ? null : expectResult.token.getIdentContent();

        expectResult = expect(anchors.add(ClassDeclaration.first(), RightCurlyBracket), TokenType.LeftCurlyBracket);
        error |= expectResult.isError;

        expectNoConsume(anchors, ClassMember.first(), RightCurlyBracket);

        while (ClassMember.firstContains(token.type)) {
            var ast = parseClassMember(anchors.add(RightCurlyBracket));
            switch (ast) {
                case Field field -> fields.add(field);
                case Method method -> methods.add(method);
                case null, default -> error = true;
            }

            expectNoConsume(anchors, ClassMember.first(), RightCurlyBracket);
        }

        expectResult = expect(anchors, RightCurlyBracket);
        error |= expectResult.isError;

        return new Class(identifier, fields, methods).makeError(error);
    }

    private AstNode parseClassMember(TokenSet anchors) {
        // This parses methods until the left paren, including
        boolean isStatic = false;

        var expectResult = expect(anchors.add(Type.first(), Identifier, SemiColon, LeftParen), TokenType.Public);
        var error = expectResult.isError;

        expectNoConsume(anchors.add(Type.first(), Identifier, SemiColon, LeftParen), Static, Type.first());

        if (token.type == TokenType.Static) {
            assertExpect(TokenType.Static);
            isStatic = true;
        }

        var type = parseType(anchors.add(Identifier, SemiColon, LeftParen));

        expectResult = expect(anchors.add(SemiColon, LeftParen), Identifier);
        error |= expectResult.isError;
        var ident = expectResult.isError ? null : expectResult.token.getIdentContent();

        expectResult = expectNoConsume(anchors, SemiColon, LeftParen);
        error |= expectResult.isError;

        switch (token.type) {
            case SemiColon -> {
                assertExpect(SemiColon);
                // TODO: if isStatic true report error;
                return new Field(ident, type).makeError(error);
            }
            case LeftParen -> {
                assertExpect(LeftParen);
                return parseMethod(anchors, type, ident, isStatic).makeError(error);
            }
            case null, default -> {
                return null;
            }
        }
    }

    private Method parseMethod(TokenSet anchors, Type returnType, String name, boolean isStatic) {
        var parameters = new ArrayList<Parameter>();

        boolean error = false;

        if (isStatic) {
            // Handling MainMethod, parse up to the closing paren,
            // rest will be parsed together
            var parameter = parseParameter(anchors.add(RightParen, MethodRest.first(), Block.first()));
            parameters.add(parameter);
        } else {
            var expectResult = expectNoConsume(anchors.add(Comma, RightParen, MethodRest.first()), Parameter.first(), RightParen);
            error |= expectResult.isError;

            if (Parameter.firstContains(token.type)) {
                var parameter = parseParameter(anchors.add(Comma, Parameter.first(), RightParen, MethodRest.first(), Block.first()));
                error |= parameter == null;
                parameters.add(parameter);
            }

            expectNoConsume(anchors.add(RightParen, MethodRest.first(), Block.first()), Comma, RightParen);

            while (token.type == Comma) {
                assertExpect(Comma);
                var parameter = parseParameter(anchors.add(Comma, Parameter.first(), RightParen, MethodRest.first(), Block.first()));
                error |= parameter == null;
                parameters.add(parameter);

                expectNoConsume(anchors.add(RightParen, MethodRest.first(), Block.first()), Comma, RightParen);
            }
        }

        var expectResult = expect(anchors.add(MethodRest.first(), Block.first()), RightParen);
        error |= expectResult.isError;

        expectNoConsume(anchors, MethodRest.first(), Block.first());

        if (MethodRest.firstContains(token.type)) {
            assertExpect(Throws);

            expectResult = expect(anchors.add(Block.first()), Identifier);
            error |= expectResult.isError;
        }
        var body = parseBlock(anchors);
        return new Method(isStatic, name, returnType, parameters, body).makeError(error);
    }

    private Parameter parseParameter(TokenSet anchors) {
        var type = parseType(anchors.add(Identifier));

        var expectResult = expect(anchors, Identifier);
        var error = expectResult.isError;
        var identifier = expectResult.isError ? null : expectResult.token.getIdentContent();

        return new Parameter(type, identifier).makeError(error);
    }

    private Type parseType(TokenSet anchors) {
        var type = parseBasicType(anchors.add(LeftSquareBracket));

        expectNoConsume(anchors, LeftSquareBracket, Type.follow());

        while (token.type == TokenType.LeftSquareBracket) {
            assertExpect(LeftSquareBracket);
            var expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
            type = new ArrayType(type).makeError(expectResult.isError);

            expectNoConsume(anchors, LeftSquareBracket, Type.follow());
        }
        return type;
    }

    private Type parseBasicType(TokenSet anchors) {
        expectNoConsume(anchors, Int, Boolean, Void, Identifier);

        switch (token.type) {
            case Int -> {
                assertExpect(Int);
                return new IntType();
            }
            case Boolean -> {
                assertExpect(Int);
                return new BoolType();
            }
            case Void -> {
                assertExpect(Void);
                return new VoidType();
            }
            case Identifier -> {
                var identifier = assertExpect(Identifier);
                return new ClassType(identifier.getIdentContent());
            }
            default -> {
                return null;
            }
        }
    }

    private Block parseBlock(TokenSet anchors) {
        var statements = new ArrayList<Statement>();

        var expectResult = expect(anchors.add(BlockStatement.first(), RightCurlyBracket), TokenType.LeftCurlyBracket);
        var error = expectResult.isError;

        expectNoConsume(anchors, BlockStatement.first(), RightCurlyBracket);

        while (BlockStatement.firstContains(token.type)) {
            var ast = parseStatement(anchors.add(BlockStatement.first(), RightCurlyBracket));
            error |= ast == null;
            statements.add(ast);

            expectNoConsume(anchors, BlockStatement.first(), RightCurlyBracket);
        }
        expectResult = expect(anchors, RightCurlyBracket);
        error |= expectResult.isError;

        return new Block(statements).makeError(error);
    }

    private Statement parseStatement(TokenSet anchors) {

        var expectResult = expectNoConsume(
                anchors,
                Block.first(),
                EmptyStatement.first(),
                IfStatement.first(),
                ExpressionStatement.first(),
                WhileStatement.first(),
                ReturnStatement.first(),
                LocalVariableDeclarationStatement.first()
        );
        var error = expectResult.isError;

        Statement result;
        if (Block.firstContains(token.type)) {
            result = parseBlock(anchors);
        } else if (EmptyStatement.firstContains(token.type)) {
            result = parseEmptyStatement(anchors);
        } else if (IfStatement.firstContains(token.type)) {
            result = parseIfStatement(anchors);
        } else if (ExpressionStatement.firstContains(token.type)) {
            result = parseExpressionStatement(anchors);
        } else if (WhileStatement.firstContains(token.type)) {
            result = parseWhileStatement(anchors);
        } else if (ReturnStatement.firstContains(token.type)) {
            result = parseReturnStatement(anchors);
        } else if (LocalVariableDeclarationStatement.firstContains(token.type)) {
            result = parseLocalVariableDeclarationStatement(anchors);
        } else {
            return null;
        }

        return result.makeError(error);
    }

    private EmptyStatement parseEmptyStatement(TokenSet anchors) {
        var expectResult = expect(anchors, TokenType.SemiColon);
        return new EmptyStatement().makeError(expectResult.isError);
    }

    private IfStatement parseIfStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(LeftParen, Expression.first(), RightParen, Statement.first(), Else), If);
        var error = expectResult.isError;

        expectResult = expect(anchors.add(Expression.first(), RightParen, Statement.first(), Else), LeftParen);
        error |= expectResult.isError;

        var condition = parseExpression(anchors.add(RightParen, Statement.first(), Else), 0);

        expectResult = expect(anchors.add(Statement.first(), Else), RightParen);
        error |= expectResult.isError;

        var thenStatement = parseStatement(anchors.add(Else));

        Optional<Statement> elseStatement = Optional.empty();

        expectNoConsume(anchors, Else, IfStatement.follow());

        if (token.type == TokenType.Else) {
            assertExpect(Else);
            elseStatement = Optional.of(parseStatement(anchors));
        }

        return new IfStatement(condition, thenStatement, elseStatement).makeError(error);
    }

    private ExpressionStatement parseExpressionStatement(TokenSet anchors) {
        var expression = parseExpression(anchors.add(SemiColon), 0);

        var expectResult = expect(anchors, SemiColon);
        var error = expectResult.isError;

        return new ExpressionStatement(expression).makeError(error);
    }

    WhileStatement parseWhileStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(LeftParen, Expression.first(), RightParen, Statement.first()), While);
        var error = expectResult.isError;

        expectResult = expect(anchors.add(Expression.first(), RightParen, Statement.first()), LeftParen);
        error |= expectResult.isError;

        var condition = parseExpression(anchors.add(RightParen, Statement.first()), 0);
        expectResult = expect(anchors.add(Statement.first()), RightParen);
        error |= expectResult.isError;

        var body = parseStatement(anchors);
        return new WhileStatement(condition, body).makeError(error);
    }

    private ReturnStatement parseReturnStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(Expression.first(), SemiColon), TokenType.Return);
        var error = expectResult.isError;

        Optional<Expression> expression = Optional.empty();

        expectNoConsume(anchors, Expression.first(), SemiColon);

        if (Expression.firstContains(token.type)) {
            expression = Optional.of(parseExpression(anchors.add(SemiColon), 0));
        }

        expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;
        return new ReturnStatement(expression).makeError(error);
    }

    private LocalVariableDeclarationStatement parseLocalVariableDeclarationStatement(TokenSet anchors) {
        var type = parseType(anchors.add(Identifier, Assign, SemiColon));

        var expectResult = expect(anchors.add(Assign, SemiColon), Identifier);
        var error = expectResult.isError;
        var ident = expectResult.isError ? null : expectResult.token.getIdentContent();

        expectNoConsume(anchors, Assign, SemiColon);

        Optional<Expression> initializer = Optional.empty();
        if (token.type == Assign) {
            assertExpect(Assign);
            initializer = Optional.of(parseExpression(anchors.add(SemiColon), 0));
        }

        expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;

        return new LocalVariableDeclarationStatement(type, ident, initializer).makeError(error);
    }

    Expression parseExpression(TokenSet anchors, int minPrec) {

        var result = this.parseUnaryExpression(anchors.add(getTokensWithHigherPrecendence(minPrec)));

        var error = false;
        expectNoConsume(anchors, BINARY_OPERATORS, Expression.follow());

        while (getBinOpPrecedence(token.type) >= minPrec) {
            var tokenPrec = getBinOpPrecedence(token.type) + 1;

            var expectResult = expect(anchors, getTokensWithHigherPrecendence(minPrec));
            error |= expectResult.isError;
            var oldToken = expectResult.token;

            var rhs = this.parseExpression(anchors.add(getTokensWithLowerPrecendence(minPrec)), tokenPrec);
            result = constructBinOpExpression(result, oldToken.type, rhs).makeError(error);

            expectNoConsume(anchors, BINARY_OPERATORS, Expression.follow());
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

    private static TokenSet getTokensWithHigherPrecendence(int prec) {
        EnumSet<TokenType> set = BINARY_OPERATORS.getSet()
                .stream()
                .filter(tokenType -> getBinOpPrecedence(tokenType) >= prec)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TokenType.class)));
        return new TokenSet(set);
    }

    private static TokenSet getTokensWithLowerPrecendence(int prec) {
        EnumSet<TokenType> set = BINARY_OPERATORS.getSet()
                .stream()
                .filter(tokenType -> getBinOpPrecedence(tokenType) < prec)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TokenType.class)));
        return new TokenSet(set);
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

    private Expression parseUnaryExpression(TokenSet anchors) {

        expectNoConsume(anchors, PostfixExpression.first(), Not, Subtract);

        if (PostfixExpression.firstContains(token.type)) {
            return parsePostfixExpression(anchors);
        }
        if (token.type == TokenType.Not) {
            assertExpect(Not);
            return new UnaryExpression(parseUnaryExpression(anchors), compiler.ast.UnaryExpression.UnaryOp.LogicalNot);
        }
        if (token.type == TokenType.Subtract) {
            assertExpect(Subtract);
            return new UnaryExpression(parseUnaryExpression(anchors), compiler.ast.UnaryExpression.UnaryOp.Negate);
        }
        return null;
    }

    private Expression parsePostfixExpression(TokenSet anchors) {

        Expression expression = parsePrimaryExpression(anchors.add(Dot, LeftSquareBracket));

        expectNoConsume(anchors, Dot, LeftSquareBracket, PostfixExpression.follow());

        while (true) {
            if (token.type == TokenType.Dot) {
                assertExpect(TokenType.Dot);

                var expectResult = expect(anchors.add(LeftParen), Identifier);
                var error = expectResult.isError;
                var ident = expectResult.isError ? null : expectResult.token.getIdentContent();

                if (token.type == LeftParen) {
                    assertExpect(LeftParen);

                    var arguments = parseArguments(anchors.add(RightParen));
                    error |= arguments.error;

                    expectResult = expect(anchors, RightParen);
                    error |= expectResult.isError;

                    expression = new MethodCallExpression(Optional.of(expression), ident, arguments.result).makeError(error);
                } else {
                    expression = new FieldAccessExpression(expression, ident).makeError(error);
                }
            } else if (token.type == LeftSquareBracket) {
                assertExpect(LeftSquareBracket);

                var inner = parseExpression(anchors.add(RightSquareBracket), 0);
                var expectResult = expect(anchors, RightSquareBracket);
                var error = expectResult.isError;

                expression = new ArrayAccessExpression(expression, inner).makeError(error);
            } else {
                return expression;
            }

            expectNoConsume(anchors, Dot, LeftSquareBracket, PostfixExpression.follow());
        }
    }

    private record ParseArgumentsResult(ArrayList<Expression> result, boolean error) {
    }

    private ParseArgumentsResult parseArguments(TokenSet anchors) {
        ArrayList<Expression> arguments = new ArrayList<>();
        var error = false;

        expectNoConsume(anchors, Expression.first(), Arguments.follow());

        if (Expression.firstContains(token.type)) {
            var expr = parseExpression(anchors.add(Comma), 0);
            error |= expr == null;
            arguments.add(expr);

            expectNoConsume(anchors, Comma, Arguments.follow());
            while (token.type == Comma) {
                assertExpect(Comma);
                expr = parseExpression(anchors.add(Comma), 0);
                error |= expr == null;
                arguments.add(expr);

                expectNoConsume(anchors, Comma, Arguments.follow());
            }
        }
        return new ParseArgumentsResult(arguments, error);
    }

    private Expression parsePrimaryExpression(TokenSet anchors) {
        expectNoConsume(anchors, Null, False, True, IntLiteral, Identifier, This, LeftParen, New);

        switch (token.type) {
            case Null -> {
                assertExpect(Null);
                return new NullExpression();
            }
            case False -> {
                assertExpect(False);
                return new BoolLiteral(false);
            }
            case True -> {
                assertExpect(True);
                return new BoolLiteral(true);
            }
            case IntLiteral -> {
                var token = assertExpect(IntLiteral);
                long value = token.getIntLiteralContent();
                return new IntLiteral(value);
            }
            case Identifier -> {
                var token = assertExpect(Identifier);
                String ident = token.getIdentContent();

                expectNoConsume(anchors, LeftParen, PrimaryExpression.follow());

                if (token.type == LeftParen) {
                    assertExpect(LeftParen);

                    var arguments = parseArguments(anchors.add(RightParen));
                    var error = arguments.error;

                    var expectResult = expect(anchors, RightParen);
                    error |= expectResult.isError;

                    return new MethodCallExpression(Optional.empty(), ident, arguments.result).makeError(error);
                }
                return new Reference(ident);
            }
            case This -> {
                assertExpect(This);
                return new ThisExpression();
            }
            case LeftParen -> {
                assertExpect(LeftParen);
                var expression = parseExpression(anchors.add(RightParen), 0);
                var expectResult = expect(anchors, RightParen);
                var error = expectResult.isError;
                return expression.makeError(error);
            }
            case New -> {
                assertExpect(New);

                var newObjExpr = parseNewObjectOrArrayExpression(anchors);

                return newObjExpr;
            }
            case null, default -> {
                return null; // Token is in anchor set.
            }
        }
    }

    private Expression parseNewObjectOrArrayExpression(TokenSet anchors) {
        // new keyword has been parsed already.

        expectNoConsume(anchors.add(LeftParen, LeftSquareBracket), BasicType.first(), Identifier);

        if (token.type == Identifier) {
            var identToken = assertExpect(Identifier);

            expectNoConsume(anchors, LeftParen, LeftSquareBracket);

            switch (token.type) {
                case LeftParen -> {
                    assertExpect(LeftParen);

                    var expectResult = expect(anchors, RightParen);
                    var error = expectResult.isError;

                    return new NewObjectExpression(identToken.getIdentContent()).makeError(error);
                }
                case LeftSquareBracket -> {
                    var type = new ClassType(identToken.getIdentContent());
                    return parseNewArrayExpression(anchors, type);
                }
                default -> {
                    return null;
                }
            }

        } else if (BasicType.firstContains(token.type)) {
            var type = parseBasicType(anchors);
            return parseNewArrayExpression(anchors, type);
        } else {
            return null;
        }
    }

    private Expression parseNewArrayExpression(TokenSet anchors, Type type) {
        // Only needs to parse [Expression]([])*.
        // new and the basic type have been parsed by the caller.

        int dimensions = 1;

        var expectResult = expect(anchors.add(Expression.first(), RightSquareBracket), LeftSquareBracket);
        var error = expectResult.isError;

        Expression expression = parseExpression(anchors.add(RightSquareBracket), 0);

        expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
        error |= expectResult.isError;

        expectNoConsume(anchors, LeftSquareBracket, NewArrayExpression.follow());

        while (token.type == LeftSquareBracket) {
            assertExpect(LeftSquareBracket);
            expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
            error |= expectResult.isError;
            dimensions++;

            expectNoConsume(anchors, LeftSquareBracket, NewArrayExpression.follow());
        }
        return new NewArrayExpression(type, expression, dimensions).makeError(error);
    }

}
