package compiler;

import compiler.ast.BinaryOpExpression;
import compiler.ast.Class;
import compiler.ast.IntLiteral;
import compiler.ast.Program;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestParser {

    @Test
    public void simpleExpressionTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.intLiteral(123, null),
                Token.operator(TokenType.Add, null),
                Token.intLiteral(456, null),
                Token.operator(TokenType.EOF, null)
        );

        var parser = new Parser(lexer);
        var ast = parser.parseExpression(0);
        var reference = new BinaryOpExpression(new IntLiteral(123), BinaryOpExpression.BinaryOp.Addition, new IntLiteral(456));
        assertTrue(reference.syntacticEq(ast));
    }

    @Test
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
        var ast = parser.parseExpression(0);
        var reference = new BinaryOpExpression(
                new BinaryOpExpression(new IntLiteral(123), BinaryOpExpression.BinaryOp.Addition,
                        new BinaryOpExpression(new IntLiteral(456), BinaryOpExpression.BinaryOp.Multiplication, new IntLiteral(789))),
                BinaryOpExpression.BinaryOp.Addition,
                new IntLiteral(385));
        assertTrue(reference.syntacticEq(ast));
    }

    @Test
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
    }

    @Test
    public void classNoIdentTest() {
        var lexer = mock(Lexer.class);
        when(lexer.nextToken()).thenReturn(
                Token.keyword(TokenType.Class, null),
                Token.keyword(TokenType.LeftCurlyBracket, null),
                Token.keyword(TokenType.RightCurlyBracket, null),
                Token.eof(null)
        );

        var parser = new Parser(lexer);
        AssertionError error = assertThrows(AssertionError.class, () -> {
            var ast = parser.parse();
        });
        String topmostMethod = error.getStackTrace()[0].getMethodName();
        assertEquals("getIdentContent", topmostMethod);
    }

}
