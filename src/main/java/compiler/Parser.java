package compiler;

import compiler.ast.Class;
import compiler.ast.*;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
        this.token = lexer.peekToken();
        this.errorMode = false;
        this.successfulParse = true;
    }

    private void addToLexer(Token token) {
        this.lexer.addSyntheticToken(token);
        this.token = token;
    }

    private record ExpectResult(Token token, boolean isError) {
    }

    private ExpectResult expect(TokenSet anchors, TokenSetLike... type) {
        return this.expectInternal(anchors, true, type);
    }

    private Token assertExpect(TokenType type) {
        assert this.token.type == type;
        var oldToken = this.token;
        this.lexer.nextToken();
        this.token = this.lexer.peekToken();
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
            lexer.nextToken();
            this.token = lexer.peekToken();
        }
        this.errorMode = false;
        return new ExpectResult(oldToken, error);
    }

    private void skipUntilAnchorSet(TokenSet anchors) {
        while (!anchors.contains(this.lexer.peekToken().type)) {
            this.lexer.nextToken();
        }
        this.token = this.lexer.peekToken();
    }

    public Program parse() {
        return parseS(TokenSet.empty());
    }

    private Program parseS(TokenSet anchors) {

        // TODO: don't ignore errors here;
        var expectResult = expectNoConsume(anchors, Program.first());
        var error = expectResult.isError;
        var ast = parseProgram(anchors.add(EOF));

        expectResult = expect(anchors, EOF);
        error |= expectResult.isError;

        return ast;
    }

    private Program parseProgram(TokenSet anchors) {
        var classes = new ArrayList<Class>();

        var expectResult = expectNoConsume(anchors, ClassDeclaration.first(), ClassDeclaration.follow());
        var error = expectResult.isError;

        while (ClassDeclaration.firstContains(token.type)) {
            var ast = parseClassDeclaration(anchors);
            classes.add(ast);

            this.expectNoConsume(anchors, ClassDeclaration.first(), ClassDeclaration.follow());
        }

        return new Program(classes).makeError(error);
    }

    private Class parseClassDeclaration(TokenSet anchors) {
        var fields = new ArrayList<Field>();
        var methods = new ArrayList<Method>();

        var expectResult = expect(anchors.add(Identifier, LeftCurlyBracket, ClassDeclaration.first(), RightCurlyBracket), Class);
        var error = expectResult.isError;

        expectResult = expect(anchors.add(LeftCurlyBracket, ClassDeclaration.first(), RightCurlyBracket), Identifier);
        error |= expectResult.isError;
        var identifier = expectResult.isError ? null : expectResult.token.getIdentContent();

        expectResult = expect(anchors.add(ClassDeclaration.first(), RightCurlyBracket), LeftCurlyBracket);
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, ClassMember.first(), RightCurlyBracket);
        error |= expectResult.isError;

        while (ClassMember.firstContains(token.type)) {
            var ast = parseClassMember(anchors.add(RightCurlyBracket));
            switch (ast) {
                case Field field -> fields.add(field);
                case Method method -> methods.add(method);
                case null, default -> error = true;
            }

            expectResult = expectNoConsume(anchors, ClassMember.first(), RightCurlyBracket);
            error |= expectResult.isError;
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

        expectResult = expectNoConsume(anchors.add(Type.first(), Identifier, SemiColon, LeftParen), Static, Type.first());
        error |= expectResult.isError;

        if (token.type == TokenType.Static) {
            assertExpect(TokenType.Static);
            isStatic = true;
        }

        expectResult = expectNoConsume(anchors.add(Identifier, SemiColon, LeftParen), Type.first());
        error |= expectResult.isError;

        var parseTypeResult = parseType(anchors.add(Identifier, SemiColon, LeftParen));
        error |= parseTypeResult.parentError;
        var type = parseTypeResult.type;

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

                expectResult = expectNoConsume(anchors, Parameter.first(), RightParen);
                error |= expectResult.isError();
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
                expectResult = expectNoConsume(anchors.add(Comma, Parameter.first(), RightParen, MethodRest.first(), Block.first()), Parameter.first());
                error |= expectResult.isError;
                var parameter = parseParameter(anchors.add(Comma, Parameter.first(), RightParen, MethodRest.first(), Block.first()));
                parameters.add(parameter);
            }

            expectResult = expectNoConsume(anchors.add(RightParen, MethodRest.first(), Block.first()), Comma, RightParen);
            error |= expectResult.isError;

            while (token.type == Comma) {
                assertExpect(Comma);
                var parameter = parseParameter(anchors.add(Comma, Parameter.first(), RightParen, MethodRest.first(), Block.first()));
                parameters.add(parameter);

                expectResult = expectNoConsume(anchors.add(RightParen, MethodRest.first(), Block.first()), Comma, RightParen);
                error |= expectResult.isError;
            }
        }

        var expectResult = expect(anchors.add(MethodRest.first(), Block.first()), RightParen);
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, MethodRest.first(), Block.first());
        error |= expectResult.isError;

        if (MethodRest.firstContains(token.type)) {
            assertExpect(Throws);

            expectResult = expect(anchors.add(Block.first()), Identifier);
            error |= expectResult.isError;
        }

        expectResult = expectNoConsume(anchors, Block.first());
        error |= expectResult.isError;
        var body = parseBlock(anchors);

        return new Method(isStatic, name, returnType, parameters, body).makeError(error);
    }

    private Parameter parseParameter(TokenSet anchors) {
        var parseTypeResult = parseType(anchors.add(Identifier));
        var error = parseTypeResult.parentError;
        var type = parseTypeResult.type;

        var expectResult = expect(anchors, Identifier);
        error |= expectResult.isError;
        var identifier = expectResult.isError ? null : expectResult.token.getIdentContent();

        return new Parameter(type, identifier).makeError(error);
    }

    private record ParseTypeResult(Type type, boolean parentError) {
    }

    private ParseTypeResult parseType(TokenSet anchors) {
        var type = parseBasicType(anchors.add(LeftSquareBracket));

        var expectResult = expectNoConsume(anchors, LeftSquareBracket, Type.follow());
        var error = expectResult.isError;

        while (token.type == TokenType.LeftSquareBracket) {
            assertExpect(LeftSquareBracket);
            expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
            error |= expectResult.isError;

            type = new ArrayType(type).makeError(error);

            expectResult = expectNoConsume(anchors, LeftSquareBracket, Type.follow());
            error = expectResult.isError; /* Not |= because we either start parsing a new ArrayType or consume garbage tokens
                                             whose error should be handled by the parent. */
        }
        return new ParseTypeResult(type, error);
    }

    private Type parseBasicType(TokenSet anchors) {
        expectNoConsume(anchors, Int, Boolean, Void, Identifier);

        switch (token.type) {
            case Int -> {
                assertExpect(Int);
                return new IntType();
            }
            case Boolean -> {
                assertExpect(Boolean);
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

        var expectResult = expect(anchors.add(BlockStatement.first(), RightCurlyBracket), LeftCurlyBracket);
        var error = expectResult.isError;

        expectResult = expectNoConsume(anchors, BlockStatement.first(), RightCurlyBracket);
        error |= expectResult.isError;

        while (BlockStatement.firstContains(token.type)) {
            var ast = parseStatement(anchors.add(BlockStatement.first(), RightCurlyBracket));
            statements.add(ast);

            expectResult = expectNoConsume(anchors, BlockStatement.first(), RightCurlyBracket);
            error |= expectResult.isError;
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
        if (token.type == Identifier) {
            result = parseExpressionStatementOrLocalVariableDeclarationStatement(anchors);
        } else if (Block.firstContains(token.type)) {
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

        expectResult = expectNoConsume(anchors.add(RightParen, Statement.first(), Else), Expression.first());
        error |= expectResult.isError;
        var expressionResult = parseExpression(anchors.add(RightParen, Statement.first(), Else), 0);
        error |= expressionResult.parentError;
        var condition = expressionResult.expression;

        expectResult = expect(anchors.add(Statement.first(), Else), RightParen);
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors.add(Else), Statement.first());
        error |= expectResult.isError;
        var thenStatement = parseStatement(anchors.add(Else));

        Optional<Statement> elseStatement = Optional.empty();

        expectNoConsume(anchors, Else, IfStatement.follow());

        if (token.type == TokenType.Else) {
            assertExpect(Else);

            expectResult = expectNoConsume(anchors, Statement.first());
            error |= expectResult.isError;
            elseStatement = Optional.of(parseStatement(anchors));
        }

        return new IfStatement(condition, thenStatement, elseStatement).makeError(error);
    }

    private ExpressionStatement parseExpressionStatement(TokenSet anchors) {
        var expressionResult = parseExpression(anchors.add(SemiColon), 0);
        var error = expressionResult.parentError;
        var expression = expressionResult.expression;

        var expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;

        return new ExpressionStatement(expression).makeError(error);
    }

    WhileStatement parseWhileStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(LeftParen, Expression.first(), RightParen, Statement.first()), While);
        var error = expectResult.isError;

        expectResult = expect(anchors.add(Expression.first(), RightParen, Statement.first()), LeftParen);
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors.add(RightParen, Statement.first()), Expression.first());
        error |= expectResult.isError;
        var expressionResult = parseExpression(anchors.add(RightParen, Statement.first()), 0);
        error |= expectResult.isError;
        var condition = expressionResult.expression;

        expectResult = expect(anchors.add(Statement.first()), RightParen);
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, Statement.first());
        error |= expectResult.isError;
        var body = parseStatement(anchors);

        return new WhileStatement(condition, body).makeError(error);
    }

    private ReturnStatement parseReturnStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(Expression.first(), SemiColon), TokenType.Return);
        var error = expectResult.isError;

        Optional<Expression> expression = Optional.empty();

        expectResult = expectNoConsume(anchors, Expression.first(), SemiColon);
        error |= expectResult.isError;

        if (Expression.firstContains(token.type)) {
            var expressionResult = parseExpression(anchors.add(SemiColon), 0);
            error |= expectResult.isError;
            var expr = expressionResult.expression;

            expression = Optional.of(expr);
        }

        expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;

        return new ReturnStatement(expression).makeError(error);
    }

    private LocalVariableDeclarationStatement parseLocalVariableDeclarationStatement(TokenSet anchors) {

        var parseTypeResult = parseType(anchors.add(Identifier, Assign, SemiColon));
        var error = parseTypeResult.parentError;
        var type = parseTypeResult.type;

        var expectResult = expect(anchors.add(Assign, SemiColon), Identifier);
        error |= expectResult.isError;
        var ident = expectResult.isError ? null : expectResult.token.getIdentContent();

        expectResult = expectNoConsume(anchors, Assign, SemiColon);
        error |= expectResult.isError;

        Optional<Expression> initializer = Optional.empty();
        if (token.type == Assign) {
            assertExpect(Assign);

            expectResult = expectNoConsume(anchors.add(SemiColon), Expression.first());
            error |= expectResult.isError;

            var expressionResult = parseExpression(anchors.add(SemiColon), 0);
            error |= expressionResult.parentError;
            var expression = expressionResult.expression;

            initializer = Optional.of(expression);
        }

        expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;

        return new LocalVariableDeclarationStatement(type, ident, initializer).makeError(error);
    }

    private static final TokenSet EXPRESSION_FIRST_SECOND_TOKEN = TokenSet.of(
            BINARY_OPERATORS, Not, Subtract, LeftParen, LeftSquareBracket, New,
            True, False, Null, This, IntLiteral, Identifier, Dot
    );

    private static final TokenSet EXPRESSION_FIRST_THIRD_TOKEN = TokenSet.of(
            BINARY_OPERATORS, Not, Subtract, LeftParen, LeftSquareBracket, New,
            True, False, Null, This, IntLiteral, Identifier, RightParen, RightSquareBracket, Dot
    );

    private Statement parseExpressionStatementOrLocalVariableDeclarationStatement(TokenSet anchors) {
        var savedIdentifier = assertExpect(Identifier);

        // Discard tokens that can not be the second token of an Expression or Type.
        var expectResult = expectNoConsume(anchors, anchors, EXPRESSION_FIRST_SECOND_TOKEN, LeftSquareBracket, Identifier);
        var error = expectResult.isError;

        if (token.type == LeftSquareBracket) {
            var savedLeftSquareBracket = assertExpect(LeftSquareBracket);

            expectResult = expectNoConsume(anchors, anchors, EXPRESSION_FIRST_THIRD_TOKEN, LeftSquareBracket, Identifier);
            error = expectResult.isError;

            if (token.type == RightSquareBracket) {
                addToLexer(savedIdentifier);
                addToLexer(savedLeftSquareBracket);
                return parseLocalVariableDeclarationStatement(anchors).makeError(error);
            } else {
                addToLexer(savedIdentifier);
                addToLexer(savedLeftSquareBracket);
                return parseExpressionStatement(anchors).makeError(error);
            }
        } else if (token.type == Identifier) {
            addToLexer(savedIdentifier);
            return parseLocalVariableDeclarationStatement(anchors).makeError(error);
        } else {
            addToLexer(savedIdentifier);
            return parseExpressionStatement(anchors).makeError(error);
        }
    }

    record ParseExpressionResult(Expression expression, boolean parentError) {
    }

    ParseExpressionResult parseExpression(TokenSet anchors, int minPrec) {
        var expressionResult = this.parseUnaryExpression(anchors.add(getTokensWithHigherPrecendence(minPrec)));
        var result = expressionResult.expression;
        var parentError = expressionResult.parentError;

        var expectResult = expectNoConsume(anchors, BINARY_OPERATORS, Expression.follow());
        parentError |= expectResult.isError;

        while (getBinOpPrecedence(token.type) >= minPrec) {
            var error = parentError;

            var tokenPrec = getBinOpPrecedence(token.type) + 1;

            expectResult = expect(anchors, getTokensWithHigherPrecendence(minPrec));
            error |= expectResult.isError;
            var oldToken = expectResult.token;

            expectResult = expectNoConsume(anchors.add(getTokensWithLowerPrecendence(minPrec)), Expression.first());
            error |= expectResult.isError;
            expressionResult = this.parseExpression(anchors.add(getTokensWithLowerPrecendence(minPrec)), tokenPrec);
            error |= expressionResult.parentError;
            var rhs = expressionResult.expression;

            result = constructBinOpExpression(result, oldToken.type, rhs).makeError(error);

            expectResult = expectNoConsume(anchors, BINARY_OPERATORS, Expression.follow());
            parentError = expectResult.isError; // not |= because the error might need to be handled by the parent.
        }

        return new ParseExpressionResult(result, parentError);
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

    private ParseExpressionResult parseUnaryExpression(TokenSet anchors) {

        if (PostfixExpression.firstContains(token.type)) {
            return parsePostfixExpression(anchors);
        }
        if (token.type == TokenType.Not) {
            assertExpect(Not);

            var expectResult = expectNoConsume(anchors, UnaryExpression.first());
            var error = expectResult.isError;

            var parseExpressionResult = parseUnaryExpression(anchors);
            var child = parseExpressionResult.expression;
            var parentError = parseExpressionResult.parentError;

            Expression expr = new UnaryExpression(child, compiler.ast.UnaryExpression.UnaryOp.LogicalNot).makeError(error);

            return new ParseExpressionResult(expr, parentError);
        }
        if (token.type == TokenType.Subtract) {
            assertExpect(Subtract);

            var expectResult = expectNoConsume(anchors, UnaryExpression.first());
            var error = expectResult.isError;
            var parseExpressionResult = parseUnaryExpression(anchors);
            var child = parseExpressionResult.expression;
            var parentError = parseExpressionResult.parentError;

            Expression expr = new UnaryExpression(child, compiler.ast.UnaryExpression.UnaryOp.Negate).makeError(error);

            return new ParseExpressionResult(expr, parentError);
        }
        return new ParseExpressionResult(null, true);
    }

    private ParseExpressionResult parsePostfixExpression(TokenSet anchors) {

        var expressionResult = parsePrimaryExpression(anchors.add(Dot, LeftSquareBracket));
        var error = expressionResult.parentError;
        var expression = expressionResult.expression;

        var expectResult = expectNoConsume(anchors, Dot, LeftSquareBracket, PostfixExpression.follow());
        error |= expectResult.isError;

        while (true) {
            if (token.type == TokenType.Dot) {
                assertExpect(TokenType.Dot);

                expectResult = expect(anchors.add(LeftParen), Identifier);
                error = expectResult.isError;
                var ident = expectResult.isError ? null : expectResult.token.getIdentContent();

                if (token.type == LeftParen) {
                    assertExpect(LeftParen);

                    expectResult = expectNoConsume(anchors.add(RightParen), Arguments.first(), RightParen); // We might consume no tokens and still parse successfully.
                    error |= expectResult.isError;
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

                expectResult = expectNoConsume(anchors.add(RightSquareBracket), Expression.first());
                expressionResult = parseExpression(anchors.add(RightSquareBracket), 0);
                error |= expectResult.isError;
                var inner = expressionResult.expression;

                expectResult = expect(anchors, RightSquareBracket);
                error |= expectResult.isError;

                expression = new ArrayAccessExpression(expression, inner).makeError(error);
            } else {
                return new ParseExpressionResult(expression, error);
            }

            expectResult = expectNoConsume(anchors, Dot, LeftSquareBracket, PostfixExpression.follow());
            error = expectResult.isError;
        }
    }

    private record ParseArgumentsResult(ArrayList<Expression> result, boolean error) {
    }

    private ParseArgumentsResult parseArguments(TokenSet anchors) {
        ArrayList<Expression> arguments = new ArrayList<>();
        var error = false;

        if (Expression.firstContains(token.type)) {
            var expressionResult = parseExpression(anchors.add(Comma), 0);
            error |= expressionResult.parentError;
            var expr = expressionResult.expression;

            arguments.add(expr);

            var expectResult = expectNoConsume(anchors, Comma, Arguments.follow());
            error |= expectResult.isError;

            while (token.type == Comma) {
                assertExpect(Comma);

                expressionResult = parseExpression(anchors.add(Comma), 0);
                error |= expressionResult.parentError;
                expr = expressionResult.expression;

                arguments.add(expr);

                expectResult = expectNoConsume(anchors, Comma, Arguments.follow());
                error |= expectResult.isError;
            }
        }

        return new ParseArgumentsResult(arguments, error);
    }

    private ParseExpressionResult parsePrimaryExpression(TokenSet anchors) {
        switch (token.type) {
            case Null -> {
                assertExpect(Null);
                return new ParseExpressionResult(new NullExpression(), false);
            }
            case False -> {
                assertExpect(False);
                return new ParseExpressionResult(new BoolLiteral(false), false);
            }
            case True -> {
                assertExpect(True);
                return new ParseExpressionResult(new BoolLiteral(true), false);
            }
            case IntLiteral -> {
                var token = assertExpect(IntLiteral);
                String value = token.getIntLiteralContent();
                return new ParseExpressionResult(new IntLiteral(value), false);
            }
            case Identifier -> {
                var token = assertExpect(Identifier);
                String ident = token.getIdentContent();

                var expectResult = expectNoConsume(anchors, LeftParen, PrimaryExpression.follow());
                var error = expectResult.isError;

                if (token.type == LeftParen) {
                    assertExpect(LeftParen);

                    var arguments = parseArguments(anchors.add(RightParen));
                    error |= arguments.error;

                    expectResult = expect(anchors, RightParen);
                    error |= expectResult.isError;

                    return new MethodCallExpression(Optional.empty(), ident, arguments.result).makeError(error);
                }

                return new ParseExpressionResult(new Reference(ident), error);
            }
            case This -> {
                assertExpect(This);
                return new ParseExpressionResult(new ThisExpression(), false);
            }
            case LeftParen -> {
                assertExpect(LeftParen);
                var parseExpressionResult = parseExpression(anchors.add(RightParen), 0);
                var expression = parseExpressionResult.expression;
                var error = parseExpressionResult.parentError;

                var expectResult = expect(anchors, RightParen);
                error |= expectResult.isError;

                expression = expression.makeError(error);

                return new ParseExpressionResult(expression, false);
            }
            case New -> {
                assertExpect(New);

                var expectResult = expectNoConsume(anchors, BasicType.first(), Identifier);
                var error = expectResult.isError;

                var expressionResult = parseNewObjectOrArrayExpression(anchors);
                var parentError = expressionResult.parentError;
                Expression expression = expressionResult.expression.makeError(error);

                return new ParseExpressionResult(expression, parentError);
            }
            case null, default -> {
                return new ParseExpressionResult(null, true); // Token is in anchor set.
            }
        }
    }

    private ParseExpressionResult parseNewObjectOrArrayExpression(TokenSet anchors) {
        // new keyword has been parsed already.

        if (token.type == Identifier) {
            var identToken = assertExpect(Identifier);

            var expectResult = expectNoConsume(anchors, LeftParen, LeftSquareBracket);
            var error = expectResult.isError;

            switch (token.type) {
                case LeftParen -> {
                    assertExpect(LeftParen);

                    expectResult = expect(anchors, RightParen);
                    error |= expectResult.isError;

                    Expression expression = new NewObjectExpression(identToken.getIdentContent()).makeError(error);
                    return new ParseExpressionResult(expression, false);
                }
                case LeftSquareBracket -> {
                    var type = new ClassType(identToken.getIdentContent());

                    expectResult = expectNoConsume(anchors, LeftSquareBracket);
                    error |= expectResult.isError;

                    var expressionResult = parseNewArrayExpression(anchors, type);
                    var parentError = expressionResult.parentError;

                    Expression expression = expressionResult.expression.makeError(error);

                    return new ParseExpressionResult(expression, parentError);
                }
                default -> {
                    return new ParseExpressionResult(null, true);
                }
            }

        } else if (BasicType.firstContains(token.type)) {
            var type = parseBasicType(anchors);
            return parseNewArrayExpression(anchors, type);
        } else {
            return new ParseExpressionResult(null, true);
        }
    }

    private ParseExpressionResult parseNewArrayExpression(TokenSet anchors, Type type) {
        // Only needs to parse [Expression]([])*.
        // new and the basic type have been parsed by the caller.

        int dimensions = 1;

        var expectResult = expect(anchors.add(Expression.first(), RightSquareBracket), LeftSquareBracket);
        var error = expectResult.isError;

        expectResult = expectNoConsume(anchors.add(RightSquareBracket), Expression.first());
        error |= expectResult.isError;
        var expressionResult = parseExpression(anchors.add(RightSquareBracket), 0);
        error |= expressionResult.parentError;
        var expression = expressionResult.expression;

        expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, LeftSquareBracket, NewArrayExpression.follow());
        var parentError = expectResult.isError;

        while (token.type == LeftSquareBracket) {
            assertExpect(LeftSquareBracket);

            error |= parentError;

            expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
            error |= expectResult.isError;
            dimensions++;

            expectResult = expectNoConsume(anchors, LeftSquareBracket, NewArrayExpression.follow());
            parentError = expectResult.isError;
        }

        Expression expr = new NewArrayExpression(type, expression, dimensions).makeError(error);
        return new ParseExpressionResult(expr, parentError);
    }

    public void dotWriter(AstNode node) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("astDump.dot")))) {
            out.write("digraph {");
            out.newLine();
            recursiveWriter(out, node);
            out.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String recursiveWriter(BufferedWriter out, AstNode node) throws IOException {
        //TODO: check if error flag is set
        if (false) {
            return node.getName() + " [color=red]";
        }
        List<AstNode> children = node.getChildren();
        if (children != null) {
            for (AstNode child : children) {
                String line = node.getName() + " -> " + recursiveWriter(out, child);
                out.write(line);
                out.newLine();
            }
        }
        return node.getName();
    }


}
