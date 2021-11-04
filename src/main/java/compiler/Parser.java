package compiler;

import compiler.ast.Class;
import compiler.ast.*;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

import static compiler.Grammar.NonT.*;
import static compiler.TokenType.*;

public class Parser {

    private final Lexer lexer;
    private Token token;
    private boolean errorMode;
    public boolean successfulParse;
    private int lastErrorPos;
    private final Optional<CompilerMessageReporter> reporter;

    private Parser(Lexer lexer, Optional<CompilerMessageReporter> reporter) {
        this.lexer = lexer;
        this.token = lexer.peekToken();
        this.errorMode = false;
        this.successfulParse = true;
        this.reporter = reporter;
        this.lastErrorPos = -1;
    }

    public Parser(Lexer lexer) {
        this(lexer, Optional.empty());
    }

    public Parser(Lexer lexer, CompilerMessageReporter reporter) {
        this(lexer, Optional.of(reporter));
    }

    private void reportError(CompilerMessage error) {
        if (error instanceof CompilerError) {
            this.successfulParse = false;
        }

        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(error));
    }

    private void addToLexer(Token token) {
        this.lexer.addSyntheticToken(token);
        this.token = this.lexer.peekToken();
    }

    private record ExpectResult(Token token, boolean isError) {
    }

    private ExpectResult expect(TokenSet anchors, TokenSetLike... type) {
        return this.expectInternal(anchors, true, type);
    }

    private Token assertExpect(TokenType... type) {
        assert Arrays.stream(type).anyMatch(t -> t == this.token.type);
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

            if (!this.errorMode && this.lastErrorPos < this.token.getSpan().start()) {
                this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(new UnexpectedTokenError(this.token, type)));
            }
            this.lastErrorPos = this.token.getSpan().start();

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

        var expectResult = expectNoConsume(anchors, Program.first(), EOF);

        Program ast;

        if (!expectResult.isError && token.type == EOF) {
            reportError(new EmptyFileWarning());
            ast = new Program(List.of());
        } else {
            ast = parseProgram(anchors.add(EOF));
        }

        expect(anchors, EOF);

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
        var classToken = expectResult.token;
        var error = expectResult.isError;

        expectResult = expect(anchors.add(LeftCurlyBracket, ClassDeclaration.first(), RightCurlyBracket), Identifier);
        var identToken = expectResult.token;
        error |= expectResult.isError;

        expectResult = expect(anchors.add(ClassDeclaration.first(), RightCurlyBracket), LeftCurlyBracket);
        var openCurly = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, ClassMember.first(), RightCurlyBracket);
        error |= expectResult.isError;

        while (ClassMember.firstContains(token.type)) {
            var ast = parseClassMember(anchors.add(RightCurlyBracket, ClassMember.first()));
            switch (ast) {
                case Field field -> fields.add(field);
                case Method method -> methods.add(method);
                case null, default -> error = true; // This should never happen
            }

            expectResult = expectNoConsume(anchors, ClassMember.first(), RightCurlyBracket);
            error |= expectResult.isError;
        }

        expectResult = expect(anchors, RightCurlyBracket);
        var closeCurly = expectResult.token;
        error |= expectResult.isError;

        return new Class(classToken, identToken, openCurly, fields, methods, closeCurly).makeError(error);
    }

    private AstNode parseClassMember(TokenSet anchors) {
        // This parses methods until the left paren, including

        var expectResult = expect(anchors.add(Type.first(), Identifier, SemiColon, LeftParen), TokenType.Public);
        var publicToken = expectResult.token;
        var error = expectResult.isError;

        expectResult = expectNoConsume(anchors.add(Type.first(), Identifier, SemiColon, LeftParen), Static, Type.first());
        error |= expectResult.isError;

        Optional<Token> staticToken = Optional.empty();
        if (token.type == TokenType.Static) {
            var tok = assertExpect(TokenType.Static);
            staticToken = Optional.ofNullable(tok);
        }

        expectResult = expectNoConsume(anchors.add(Identifier, SemiColon, LeftParen), Type.first());
        error |= expectResult.isError;

        var parseTypeResult = parseType(anchors.add(Identifier, SemiColon, LeftParen));
        error |= parseTypeResult.parentError;
        var type = parseTypeResult.type;

        if (staticToken.isPresent() && !(type instanceof VoidType)) {
            reportError(new StaticMethodReturnError(type, staticToken.get()));
            error = true;
        }

        expectResult = expect(anchors.add(SemiColon, LeftParen), Identifier);
        var identToken = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, SemiColon, LeftParen);
        error |= expectResult.isError;

        switch (token.type) {
            case SemiColon -> {
                var semicolonToken = assertExpect(SemiColon);

                if (staticToken.isPresent()) {
                    reportError(new StaticFieldError(staticToken.get()));
                    error = true;
                }

                return new Field(publicToken, type, identToken, semicolonToken).makeError(error);
            }
            case LeftParen -> {
                assertExpect(LeftParen);

                expectResult = expectNoConsume(anchors, Parameter.first(), RightParen);
                error |= expectResult.isError();

                var method = parseMethod(anchors, publicToken, type, identToken, staticToken);
                if (method != null) {
                    method.makeError(error);
                }

                return method;
            }
            case null, default -> {
                if (staticToken.isPresent()) {
                    reportError(new StaticFieldError(staticToken.get()));
                    error = true;
                }

                return new Field(publicToken, type, identToken, null).makeError(error);
            }
        }
    }

    private Method parseMethod(TokenSet anchors, Token publicToken, Type returnType, Token nameIdent, Optional<Token> isStatic) {
        var parameters = new ArrayList<Parameter>();

        boolean error = false;

        if (isStatic.isPresent()) {
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

            expectResult = expectNoConsume(anchors.add(Parameter.first(), RightParen, MethodRest.first(), Block.first()), Comma, RightParen);
            error |= expectResult.isError;

            while (token.type == Comma || Parameter.firstContains(token.type)) {
                expectResult = expect(anchors.add(Parameter.first(), RightParen, MethodRest.first(), Block.first()), Comma);
                error |= expectResult.isError;

                var parameter = parseParameter(anchors.add(Comma, Parameter.first(), RightParen, MethodRest.first(), Block.first()));
                parameters.add(parameter);

                expectResult = expectNoConsume(anchors.add(Parameter.first(), RightParen, MethodRest.first(), Block.first()), Comma, RightParen);
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

        return new Method(publicToken, isStatic, nameIdent, returnType, parameters, body).makeError(error);
    }

    private Parameter parseParameter(TokenSet anchors) {
        var parseTypeResult = parseType(anchors.add(Identifier));
        var error = parseTypeResult.parentError;
        var type = parseTypeResult.type;

        var expectResult = expect(anchors, Identifier);
        var identifier = expectResult.token;
        error |= expectResult.isError;

        return new Parameter(type, identifier).makeError(error);
    }

    private record ParseTypeResult(Type type, boolean parentError) {
    }

    private ParseTypeResult parseType(TokenSet anchors) {
        var type = parseBasicType(anchors.add(LeftSquareBracket));

        var expectResult = expectNoConsume(anchors, LeftSquareBracket, Type.follow());
        var error = expectResult.isError;

        while (token.type == TokenType.LeftSquareBracket) {
            var openBracket = assertExpect(LeftSquareBracket);

            expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
            var closedBracket = expectResult.token;
            error |= expectResult.isError;

            type = new ArrayType(type, openBracket, closedBracket).makeError(error);

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
                var intToken = assertExpect(Int);
                return new IntType(intToken);
            }
            case Boolean -> {
                var boolToken = assertExpect(Boolean);
                return new BoolType(boolToken);
            }
            case Void -> {
                var voidToken = assertExpect(Void);
                return new VoidType(voidToken);
            }
            case Identifier -> {
                var identifier = assertExpect(Identifier);
                return new ClassType(identifier);
            }
            default -> {
                return null;
            }
        }
    }

    private Block parseBlock(TokenSet anchors) {
        var statements = new ArrayList<Statement>();

        var expectResult = expect(anchors.add(BlockStatement.first(), RightCurlyBracket), LeftCurlyBracket);
        var openCurly = expectResult.token;
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
        var closedCurly = expectResult.token;
        error |= expectResult.isError;

        return new Block(openCurly, statements, closedCurly).makeError(error);
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
            return null; // This should never happen
        }

        if (result != null) {
            result = result.makeError(error);
        }

        return result;
    }

    private EmptyStatement parseEmptyStatement(TokenSet anchors) {
        var expectResult = expect(anchors, TokenType.SemiColon);
        var semicolon = expectResult.token;
        return new EmptyStatement(semicolon).makeError(expectResult.isError);
    }

    private IfStatement parseIfStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(LeftParen, Expression.first(), RightParen, Statement.first(), Else), If);
        var ifToken = expectResult.token;
        var error = expectResult.isError;

        expectResult = expect(anchors.add(Expression.first(), RightParen, Statement.first(), Else), LeftParen);
        var openParen = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors.add(RightParen, Statement.first(), Else), Expression.first());
        error |= expectResult.isError;
        var expressionResult = parseExpression(anchors.add(RightParen, Statement.first(), Else), 0);
        error |= expressionResult.parentError;
        var condition = expressionResult.expression;

        expectResult = expect(anchors.add(Statement.first(), Else), RightParen);
        var closeParen = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors.add(Else), Statement.first());
        error |= expectResult.isError;
        var thenStatement = parseStatement(anchors.add(Else));

        if (thenStatement instanceof LocalVariableDeclarationStatement) {
            reportError(new InvalidLocalVariableDeclarationError(thenStatement));
            error = true;
        }

        Optional<Statement> elseStatement = Optional.empty();

        expectNoConsume(anchors, Else, IfStatement.follow());

        if (token.type == TokenType.Else) {
            assertExpect(Else);

            expectResult = expectNoConsume(anchors, Statement.first());
            error |= expectResult.isError;
            var elseStmt = parseStatement(anchors);

            if (elseStmt instanceof LocalVariableDeclarationStatement) {
                reportError(new InvalidLocalVariableDeclarationError(elseStmt));
                error = true;
            }

            elseStatement = Optional.ofNullable(elseStmt);
        }

        return new IfStatement(ifToken, openParen, condition, closeParen, thenStatement, elseStatement).makeError(error);
    }

    private ExpressionStatement parseExpressionStatement(TokenSet anchors) {
        var expressionResult = parseExpression(anchors.add(SemiColon), 0);
        var error = expressionResult.parentError;
        var expression = expressionResult.expression;

        var expectResult = expect(anchors, SemiColon);
        var semicolon = expectResult.token;
        error |= expectResult.isError;

        return new ExpressionStatement(expression, semicolon).makeError(error);
    }

    private WhileStatement parseWhileStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(LeftParen, Expression.first(), RightParen, Statement.first()), While);
        var whileToken = expectResult.token;
        var error = expectResult.isError;

        expectResult = expect(anchors.add(Expression.first(), RightParen, Statement.first()), LeftParen);
        var openParen = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors.add(RightParen, Statement.first()), Expression.first());
        error |= expectResult.isError;
        var expressionResult = parseExpression(anchors.add(RightParen, Statement.first()), 0);
        error |= expectResult.isError;
        var condition = expressionResult.expression;

        expectResult = expect(anchors.add(Statement.first()), RightParen);
        var closeParen = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, Statement.first());
        error |= expectResult.isError;
        var body = parseStatement(anchors);

        if (body instanceof LocalVariableDeclarationStatement) {
            reportError(new InvalidLocalVariableDeclarationError(body));
            error = true;
        }

        return new WhileStatement(whileToken, openParen, condition, closeParen, body).makeError(error);
    }

    private ReturnStatement parseReturnStatement(TokenSet anchors) {
        var expectResult = expect(anchors.add(Expression.first(), SemiColon), Return);
        var returnToken = expectResult.token;
        var error = expectResult.isError;

        Optional<Expression> expression = Optional.empty();

        expectResult = expectNoConsume(anchors, Expression.first(), SemiColon);
        error |= expectResult.isError;

        if (Expression.firstContains(token.type)) {
            var expressionResult = parseExpression(anchors.add(SemiColon), 0);
            error |= expectResult.isError;
            var expr = expressionResult.expression;

            expression = Optional.ofNullable(expr);
        }

        expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;

        return new ReturnStatement(returnToken, expression).makeError(error);
    }

    private LocalVariableDeclarationStatement parseLocalVariableDeclarationStatement(TokenSet anchors) {

        var parseTypeResult = parseType(anchors.add(Identifier, Assign, SemiColon));
        var error = parseTypeResult.parentError;
        var type = parseTypeResult.type;

        var expectResult = expect(anchors.add(Assign, SemiColon), Identifier);
        var identToken = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, Assign, SemiColon);
        error |= expectResult.isError;

        Optional<Expression> initializer = Optional.empty();
        Optional<Token> assignToken = Optional.empty();
        if (token.type == Assign) {
            assignToken = Optional.of(assertExpect(Assign));

            expectResult = expectNoConsume(anchors.add(SemiColon), Expression.first());
            error |= expectResult.isError;

            var expressionResult = parseExpression(anchors.add(SemiColon), 0);
            error |= expressionResult.parentError;
            var expression = expressionResult.expression;

            initializer = Optional.ofNullable(expression);
        }

        expectResult = expect(anchors, SemiColon);
        error |= expectResult.isError;

        return new LocalVariableDeclarationStatement(type, identToken, assignToken, initializer).makeError(error);
    }

    private static final TokenSet EXPRESSION_TOKEN_FOLLOWED_BY_IDENT = TokenSet.of(
            BINARY_OPERATORS, LeftParen, LeftSquareBracket, Dot
    );

    private static final TokenSet EXPRESSION_TOKEN_FOLLOWED_BY_IDENT_LEFTSQUAREBRACKET = TokenSet.of(
            Expression.first()
    );

    private Statement parseExpressionStatementOrLocalVariableDeclarationStatement(TokenSet anchors) {
        var savedIdentifier = assertExpect(Identifier);

        // Discard tokens that can not be the second token of an Expression or Type (beginning with an identifier).
        var expectResult = expectNoConsume(anchors, EXPRESSION_TOKEN_FOLLOWED_BY_IDENT, LeftSquareBracket, Identifier);
        var error = expectResult.isError;

        if (token.type == LeftSquareBracket) {
            var savedLeftSquareBracket = assertExpect(LeftSquareBracket);

            // Discard tokens that can not be the third token of an Expression or Type (beginning with an identifier and LeftSquareBracket).
            expectResult = expectNoConsume(anchors, EXPRESSION_TOKEN_FOLLOWED_BY_IDENT_LEFTSQUAREBRACKET, RightSquareBracket, Identifier);
            error = expectResult.isError;

            if (token.type == RightSquareBracket) {
                addToLexer(savedIdentifier);
                addToLexer(savedLeftSquareBracket);

                Statement statement = parseLocalVariableDeclarationStatement(anchors);
                if (statement != null) {
                    statement.makeError(error);
                }
                return statement;
            } else {
                addToLexer(savedIdentifier);
                addToLexer(savedLeftSquareBracket);
                Statement statement = parseExpressionStatement(anchors);
                if (statement != null) {
                    statement.makeError(error);
                }
                return statement;
            }
        } else if (token.type == Identifier) {
            addToLexer(savedIdentifier);
            Statement statement = parseLocalVariableDeclarationStatement(anchors);
            if (statement != null) {
                statement.makeError(error);
            }
            return statement;
        } else {
            addToLexer(savedIdentifier);
            Statement statement = parseExpressionStatement(anchors);
            if (statement != null) {
                statement.makeError(error);
            }
            return statement;
        }
    }

    record ParseExpressionResult(Expression expression, boolean parentError) {
    }

    private ParseExpressionResult parseExpression(TokenSet anchors, int minPrec) {
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

            result = constructBinOpExpression(result, oldToken, rhs).makeError(error);

            expectResult = expectNoConsume(anchors, BINARY_OPERATORS, Expression.follow());
            parentError = expectResult.isError; // not |= because the error might need to be handled by the parent.
        }

        return new ParseExpressionResult(result, parentError);
    }

    private static Expression constructBinOpExpression(Expression lhs, Token token, Expression rhs) {
        if (token.type == TokenType.Assign) {
            return new AssignmentExpression(lhs, token, rhs);
        } else {
            return new BinaryOpExpression(lhs, token, rhs);
        }
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
            var notToken = assertExpect(Not);

            var expectResult = expectNoConsume(anchors, UnaryExpression.first());
            var error = expectResult.isError;

            var parseExpressionResult = parseUnaryExpression(anchors);
            var child = parseExpressionResult.expression;
            var parentError = parseExpressionResult.parentError;

            Expression expr = new UnaryExpression(child, notToken).makeError(error);

            return new ParseExpressionResult(expr, parentError);
        }
        if (token.type == TokenType.Subtract) {
            var minusToken = assertExpect(Subtract);

            var expectResult = expectNoConsume(anchors, UnaryExpression.first());
            var error = expectResult.isError;
            var parseExpressionResult = parseUnaryExpression(anchors);
            var child = parseExpressionResult.expression;
            var parentError = parseExpressionResult.parentError;

            Expression expr = new UnaryExpression(child, minusToken).makeError(error);

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
                var dotToken = assertExpect(TokenType.Dot);

                expectResult = expect(anchors.add(LeftParen), Identifier);
                var identToken = expectResult.token;
                error = expectResult.isError;

                if (token.type == LeftParen) {
                    var openParen = assertExpect(LeftParen);

                    expectResult = expectNoConsume(anchors.add(RightParen), Arguments.first(), RightParen); // We might consume no tokens and still parse successfully.
                    error |= expectResult.isError;
                    var arguments = parseArguments(anchors.add(RightParen));
                    error |= arguments.error;

                    expectResult = expect(anchors, RightParen);
                    var closeParen = expectResult.token;
                    error |= expectResult.isError;

                    expression = new MethodCallExpression(Optional.ofNullable(expression), Optional.ofNullable(dotToken), identToken, openParen, arguments.result, closeParen).makeError(error);
                } else {
                    expression = new FieldAccessExpression(expression, dotToken, identToken).makeError(error);
                }
            } else if (token.type == LeftSquareBracket) {
                var openBracket = assertExpect(LeftSquareBracket);

                expectResult = expectNoConsume(anchors.add(RightSquareBracket), Expression.first());
                expressionResult = parseExpression(anchors.add(RightSquareBracket), 0);
                error |= expectResult.isError;
                var inner = expressionResult.expression;

                expectResult = expect(anchors, RightSquareBracket);
                var closedBracket = expectResult.token;
                error |= expectResult.isError;

                expression = new ArrayAccessExpression(expression, openBracket, inner, closedBracket).makeError(error);
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

            var expectResult = expectNoConsume(anchors.add(Expression.first()), Comma, Arguments.follow());
            error |= expectResult.isError;

            while (token.type == Comma || Expression.firstContains(token.type)) {
                expectResult = expect(anchors.add(Expression.first()), Comma);
                error |= expectResult.isError;

                expressionResult = parseExpression(anchors.add(Comma), 0);
                error |= expressionResult.parentError;
                expr = expressionResult.expression;

                arguments.add(expr);

                expectResult = expectNoConsume(anchors.add(Expression.first()), Comma, Arguments.follow());
                error |= expectResult.isError;
            }
        }

        return new ParseArgumentsResult(arguments, error);
    }

    private ParseExpressionResult parsePrimaryExpression(TokenSet anchors) {
        switch (token.type) {
            case Null -> {
                var nullToken = assertExpect(Null);
                return new ParseExpressionResult(new NullExpression(nullToken), false);
            }
            case True, False -> {
                var boolToken = assertExpect(True, False);
                return new ParseExpressionResult(new BoolLiteral(boolToken), false);
            }
            case IntLiteral -> {
                var token = assertExpect(IntLiteral);
                return new ParseExpressionResult(new IntLiteral(token), false);
            }
            case Identifier -> {
                var identToken = assertExpect(Identifier);

                var expectResult = expectNoConsume(anchors, LeftParen, PrimaryExpression.follow());
                var error = expectResult.isError;

                if (token.type == LeftParen) {
                    var openParen = assertExpect(LeftParen);

                    var arguments = parseArguments(anchors.add(RightParen));
                    error |= arguments.error;

                    expectResult = expect(anchors, RightParen);
                    var closeParen = expectResult.token;
                    error |= expectResult.isError;

                    Expression expression = new MethodCallExpression(Optional.empty(), Optional.empty(), identToken, openParen, arguments.result, closeParen).makeError(error);
                    return new ParseExpressionResult(expression, false);
                }

                return new ParseExpressionResult(new Reference(identToken), error);
            }
            case This -> {
                var thisToken = assertExpect(This);
                return new ParseExpressionResult(new ThisExpression(thisToken), false);
            }
            case LeftParen -> {
                assertExpect(LeftParen);
                var parseExpressionResult = parseExpression(anchors.add(RightParen), 0);
                var expression = parseExpressionResult.expression;
                var error = parseExpressionResult.parentError;

                var expectResult = expect(anchors, RightParen);
                error |= expectResult.isError;

                expression = expression;

                if (expression != null) {
                    expression.makeError(error);
                }

                return new ParseExpressionResult(expression, false);
            }
            case New -> {
                var newToken = assertExpect(New);

                var expectResult = expectNoConsume(anchors, BasicType.first(), Identifier);
                var error = expectResult.isError;

                var expressionResult = parseNewObjectOrArrayExpression(anchors, newToken);
                var parentError = expressionResult.parentError;
                Expression expression = expressionResult.expression;

                if (expression != null) {
                    expression.makeError(error);
                }

                return new ParseExpressionResult(expression, parentError);
            }
            case null, default -> {
                // This should never happen, first set further up prevents it
                return new ParseExpressionResult(null, true); // Token is in anchor set.
            }
        }
    }

    private ParseExpressionResult parseNewObjectOrArrayExpression(TokenSet anchors, Token newToken) {
        // new keyword has been parsed already.

        if (token.type == Identifier) {
            var identToken = assertExpect(Identifier);

            var expectResult = expectNoConsume(anchors, LeftParen, LeftSquareBracket);
            var error = expectResult.isError;

            switch (token.type) {
                case LeftParen -> {
                    var openParen = assertExpect(LeftParen);

                    var argumentResult = parseArguments(anchors.add(RightParen));
                    error |= argumentResult.error;

                    expectResult = expect(anchors, RightParen);
                    var closeParen = expectResult.token;
                    error |= expectResult.isError;

                    Expression expression = new NewObjectExpression(newToken, identToken, openParen, closeParen);

                    if (argumentResult.result.size() > 0) {
                        reportError(new NewObjectWithArgumentsError(expression, argumentResult.result));
                        error = true;
                    }

                    expression.makeError(error);

                    return new ParseExpressionResult(expression, false);
                }
                case LeftSquareBracket -> {
                    var type = new ClassType(identToken);

                    expectResult = expectNoConsume(anchors, LeftSquareBracket);
                    error |= expectResult.isError;

                    var expressionResult = parseNewArrayExpression(anchors, type);
                    var parentError = expressionResult.parentError;

                    Expression expression = expressionResult.expression;

                    if (expression != null) {
                        expression.makeError(error);
                    }

                    return new ParseExpressionResult(expression, parentError);
                }
                default -> {
                    return new ParseExpressionResult(new NewObjectExpression(newToken, identToken, null, null).makeError(true), true);
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
        var lastOpenBracket = expectResult.token;
        var error = expectResult.isError;

        expectResult = expectNoConsume(anchors.add(RightSquareBracket), Expression.first());
        error |= expectResult.isError;
        var expressionResult = parseExpression(anchors.add(RightSquareBracket), 0);
        error |= expressionResult.parentError;
        var expression = expressionResult.expression;

        expectResult = expect(anchors.add(LeftSquareBracket), RightSquareBracket);
        var lastCloseBracket = expectResult.token;
        error |= expectResult.isError;

        expectResult = expectNoConsume(anchors, LeftSquareBracket, NewArrayExpression.follow());
        var parentError = expectResult.isError;

        while (token.type == LeftSquareBracket) {
            lastOpenBracket = assertExpect(LeftSquareBracket);

            error |= parentError;

            // Expressions are not allowed here, but a user might write them anyway.
            expectResult = expectNoConsume(anchors.add(LeftSquareBracket, NewArrayExpression.follow()), RightSquareBracket, Expression.first());
            error |= expectResult.isError;

            if (Expression.firstContains(token.type)) {
                this.addToLexer(lastOpenBracket);
                break;
            }

            expectResult = expect(anchors.add(LeftSquareBracket, NewArrayExpression.follow()), RightSquareBracket);
            error |= expectResult.isError;
            lastCloseBracket = expectResult.token;
            dimensions++;

            expectResult = expectNoConsume(anchors, LeftSquareBracket, NewArrayExpression.follow());
            parentError = expectResult.isError;
        }

        Expression expr = new NewArrayExpression(type, expression, dimensions, lastCloseBracket).makeError(error);
        return new ParseExpressionResult(expr, parentError);
    }

    public void dotWriter(AstNode node) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("astDump.dot")))) {
            out.write("digraph {");
            out.newLine();
            recursiveWriter(out, node, "a");
            out.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String recursiveWriter(BufferedWriter out, AstNode node, String runthrough) throws IOException {
        if (node == null) {
            String line = runthrough + " [label=NULL color=red]\n";
            return runthrough;
        }
        List<AstNode> children = node.getChildren();

        String line = runthrough + " [label=\"" + node.getClass().getSimpleName() + "\n" + node.getName() + "\n";

        line += node.getSpan() + "\n";

        line += "\"";

        if (node.isError()) {
            line += " color=red";
        }
        line += "]\n";

        if (children != null) {
            int counter = 0;
            for (AstNode child : children) {
                if (child == null) {
                    line += runthrough + "a" + counter + " [label=NULL color=red]\n";
                    counter++;
                    continue;
                }
                if (child.getClass() == VoidType.class) continue;
                line += runthrough + " -> " + recursiveWriter(out, child, runthrough + "a" + counter) + "\n";
                counter++;
            }
        }
        out.write(line);
        out.newLine();
        return runthrough;
    }

}