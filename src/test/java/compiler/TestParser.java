package compiler;

import compiler.ast.BinaryOpExpression;
import compiler.ast.IntLiteral;
import org.junit.jupiter.api.Test;


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
        var ast = parser.parseExpression2(0);
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
        var ast = parser.parseExpression2(0);
        var reference = new BinaryOpExpression(
                new BinaryOpExpression(new IntLiteral(123), BinaryOpExpression.BinaryOp.Addition,
                        new BinaryOpExpression(new IntLiteral(456), BinaryOpExpression.BinaryOp.Multiplication, new IntLiteral(789))),
                BinaryOpExpression.BinaryOp.Addition,
                new IntLiteral(385));
        assertTrue(reference.syntacticEq(ast));
    }
}
