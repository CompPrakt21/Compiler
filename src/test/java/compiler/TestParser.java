package compiler;

import compiler.ast.*;
import compiler.ast.Class;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestParser {

    /*@Test
    public void simpleExpressionTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.intLiteral(123, null),
                Token.operator(TokenType.Add, null),
                Token.intLiteral(456, null),
                Token.operator(TokenType.EOF, null)
        );

        var parser = new Parser(lexer);
        var ast = parser.parseExpression(TokenSet.empty(), 0);
        var reference = new BinaryOpExpression(new IntLiteral(123), BinaryOpExpression.BinaryOp.Addition, new IntLiteral(456));
        assertTrue(reference.syntacticEq(ast.expression));
    }*/

    /*@Test
    public void mulAddExpressionTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.intLiteral(123, null),
                Token.operator(TokenType.Add, null),
                Token.intLiteral(456, null),
                Token.operator(TokenType.Multiply, null),
                Token.intLiteral(789, null),
                Token.operator(TokenType.Add, null),
                Token.intLiteral(385, null),
                Token.operator(TokenType.EOF, null)
        );

        var parser = new Parser(lexer);
        var ast = parser.parseExpression(TokenSet.empty(), 0);
        var reference = new BinaryOpExpression(
                new BinaryOpExpression(new IntLiteral(123), BinaryOpExpression.BinaryOp.Addition,
                        new BinaryOpExpression(new IntLiteral(456), BinaryOpExpression.BinaryOp.Multiplication, new IntLiteral(789))),
                BinaryOpExpression.BinaryOp.Addition,
                new IntLiteral(385));
        assertTrue(reference.syntacticEq(ast.expression));
    }*/

    /*@Test
    public void programEmptyClassTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.keyword(TokenType.Class, null),
                Token.identifier("name", null),
                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.RightCurlyBracket, null),
                Token.eof(null)
        );

        var parser = new Parser(lexer);
        var ast = parser.parse();
        var reference = new Program(List.of(new Class("name", List.of(), List.of())));
        assertTrue(reference.syntacticEq(ast));
    }*/

    /*@Test
    public void classNoIdentTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.keyword(TokenType.Class, null),
                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.RightCurlyBracket, null),
                Token.eof(null)
        );

        var parser = new Parser(lexer);
        Error error = assertThrows(Error.class, () -> {
            var ast = parser.parse();
        });
        String topmostMethod = error.getStackTrace()[0].getMethodName();
        assertEquals("expectIdent", topmostMethod);
    }*/

    /*@Test
    public void fullProgramTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.keyword(TokenType.Class, null),
                Token.identifier("name", null),
                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.Public, null),
                Token.keyword(TokenType.Int, null),
                Token.identifier("fieldName", null),
                Token.keyword(TokenType.SemiColon, null),


                Token.keyword(TokenType.Public, null),
                Token.keyword(TokenType.Void, null),
                Token.identifier("method", null),
                Token.keyword(TokenType.LeftParen, null),
                Token.identifier("SomeClass", null),
                Token.identifier("firstParam", null),
                Token.keyword(TokenType.Comma, null),
                Token.keyword(TokenType.Boolean, null),
                Token.identifier("secondParam", null),
                Token.keyword(TokenType.RightParen, null),
                Token.keyword(TokenType.Throws, null),
                Token.identifier("SomeException", null),

                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.Int, null),
                Token.identifier("localVar", null),
                Token.keyword(TokenType.SemiColon, null),
                Token.keyword(TokenType.RightCurlyBracket, null),


                Token.keyword(TokenType.Public, null),
                Token.keyword(TokenType.Static, null),
                Token.keyword(TokenType.Void, null),
                Token.identifier("main", null),
                Token.keyword(TokenType.LeftParen, null),
                Token.keyword(TokenType.Int, null),
                Token.keyword(TokenType.LeftSquareBracket, null),
                Token.keyword(TokenType.RightSquareBracket, null),
                Token.identifier("mainParam", null),
                Token.keyword(TokenType.RightParen, null),

                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.RightCurlyBracket, null),


                Token.keyword(TokenType.If, null),
                Token.keyword(TokenType.LeftParen, null),
                Token.keyword(TokenType.True, null),
                Token.keyword(TokenType.RightParen, null),
                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.RightCurlyBracket, null),
                Token.keyword(TokenType.Else, null),
                Token.keyword(TokenType.SemiColon, null),


                Token.keyword(TokenType.While, null),
                Token.keyword(TokenType.LeftParen, null),
                Token.keyword(TokenType.This, null),
                Token.keyword(TokenType.Dot, null),
                Token.identifier("field", null),
                Token.keyword(TokenType.Dot, null),
                Token.identifier("method", null),
                Token.keyword(TokenType.LeftParen, null),
                Token.keyword(TokenType.Null, null),
                Token.keyword(TokenType.Comma, null),
                Token.keyword(TokenType.Null, null),
                Token.keyword(TokenType.RightParen, null),
                Token.keyword(TokenType.RightParen, null),

                Token.keyword(TokenType.LeftParen, null),
                Token.intLiteral(1, null),
                Token.keyword(TokenType.RightParen, null),
                Token.keyword(TokenType.SemiColon, null),


                Token.keyword(TokenType.Return, null),
                Token.keyword(TokenType.Not, null),
                Token.intLiteral(1, null),
                Token.keyword(TokenType.Equals, null),
                Token.keyword(TokenType.Subtract, null),
                Token.intLiteral(3, null),
                Token.keyword(TokenType.Or, null),
                Token.keyword(TokenType.False, null),
                Token.keyword(TokenType.SemiColon, null),
                Token.keyword(TokenType.RightCurlyBracket, null),

                Token.keyword(TokenType.RightCurlyBracket, null),
                Token.eof(null)
        );

        var parser = new Parser(lexer);
        var ast = parser.parse();

        var fields = List.of(new Field("fieldName", new IntType()));
        var methods = List.of(
                new Method(
                        false,
                        "method",
                        new VoidType(),
                        List.of(
                                new Parameter(new ClassType("SomeClass"), "firstParam"),
                                new Parameter(new BoolType(), "secondParam")
                        ),
                        new Block(List.of(
                                new LocalVariableDeclarationStatement(new IntType(), "localVar", Optional.empty())
                        ))
                ),
                new Method(
                        true,
                        "main",
                        new VoidType(),
                        List.of(
                                new Parameter(new ArrayType(new IntType()), "mainParam")
                        ),
                        new Block(List.of(
                                new Block(List.of()),
                                new IfStatement(
                                        new BoolLiteral(true),
                                        new Block(List.of()),
                                        Optional.of(new EmptyStatement())
                                ),
                                new WhileStatement(
                                        new MethodCallExpression(
                                                Optional.of(new FieldAccessExpression(
                                                        new ThisExpression(),
                                                        "field"
                                                )),
                                                "method",
                                                List.of(new NullExpression(), new NullExpression())
                                        ),
                                        new ExpressionStatement(new IntLiteral(1))
                                ),
                                new ReturnStatement(Optional.of(
                                        new BinaryOpExpression(
                                                new BinaryOpExpression(
                                                        new UnaryExpression(new IntLiteral(1), UnaryExpression.UnaryOp.LogicalNot),
                                                        BinaryOpExpression.BinaryOp.Equal,
                                                        new UnaryExpression(new IntLiteral(3), UnaryExpression.UnaryOp.Negate)
                                                ),
                                                BinaryOpExpression.BinaryOp.Or,
                                                new BoolLiteral(false)
                                        )
                                ))
                        ))
                )
        );

        var reference = new Program(List.of(
                new Class("name", fields, methods)
        ));

        assertTrue(reference.syntacticEq(ast));
    }*/

    @Test
    public void myParserTest() {
        Lexer lexer = new Lexer("""
                /*class MyClass {
                                
                    public  String myField;
                                
                    public void test(int i) {
                        return ifoo + 12;
                    }
                    
                    public static void main(String[] args) {
                        int[][][] x = new int[123][][];
                        x[0] = new MyClass();
                        this.test(44);
                        return;
                    }
                }*/
                                
                class Main {
                    public static void main(String[] foo) {
                        while if else 
                    }
                }
                """);

        Parser parser = new Parser(lexer);

        var ast = parser.parse();
        parser.dotWriter(ast);
    }
}
