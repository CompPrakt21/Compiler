package compiler;

import compiler.ast.*;
import compiler.ast.Class;

import java.util.ArrayList;

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
            var paramType = parseType();
            var paramIdentifier = this.token.getIdentContent();
            expect(TokenType.Identifier);
        } else {
            methodType = parseType();
            methodIdentifier = this.token.getIdentContent();
            expect(TokenType.Identifier);
            expect(TokenType.LeftParen);
            //TODO: Parameters
        }
        expect(TokenType.RightParen);
        //TODO: MethodRest
        var body = parseBlock();
        return new Method(isStatic, methodIdentifier, methodType, null, body);
    }

    private Block parseBlock() {
        return null;
    }

    private Type parseType() {
        return null;
    }

}
