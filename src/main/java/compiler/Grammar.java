package compiler;

import java.util.Map;
import java.util.Set;

public class Grammar {

    public enum NonTerminals {
        AdditiveExpression,
        Arguments,
        ArrayAccess,
        AssignmentExpression,
        BasicType,
        Block,
        BlockStatement,
        ClassDeclaration,
        ClassMember,
        EmptyStatement,
        EqualityExpression,
        Expression,
        ExpressionStatement,
        Field,
        FieldAccess,
        IfStatement,
        LocalVariableDeclarationStatement,
        LogicalAndExpression,
        LogicalOrExpression,
        MainMethod,
        Method,
        MethodInvocation,
        MethodRest,
        MultiplicativeExpression,
        NewArrayExpression,
        NewObjectExpression,
        Parameter,
        Parameters,
        PostfixExpression,
        PostfixOp,
        PrimaryExpression,
        Program,
        RelationalExpression,
        ReturnStatement,
        S,
        Statement,
        Type,
        UnaryExpression,
        WhileStatement
    }


    public static final Map<NonTerminals, Set<TokenType>> FIRST = Map.ofEntries(

            Map.entry(NonTerminals.AdditiveExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.Arguments, Set.of(

                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.ArrayAccess, Set.of(
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.AssignmentExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.BasicType, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonTerminals.Block, Set.of(
                    TokenType.LeftCurlyBracket
            )),
            Map.entry(NonTerminals.BlockStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.ClassDeclaration, Set.of(
                    TokenType.Class
            )),
            Map.entry(NonTerminals.ClassMember, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonTerminals.EmptyStatement, Set.of(
                    TokenType.SemiColon
            )),
            Map.entry(NonTerminals.EqualityExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.Expression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.ExpressionStatement, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.Field, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonTerminals.FieldAccess, Set.of(
                    TokenType.Dot
            )),
            Map.entry(NonTerminals.IfStatement, Set.of(
                    TokenType.If
            )),
            Map.entry(NonTerminals.LocalVariableDeclarationStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonTerminals.LogicalAndExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.LogicalOrExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.MainMethod, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonTerminals.Method, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonTerminals.MethodInvocation, Set.of(
                    TokenType.Dot
            )),
            Map.entry(NonTerminals.MethodRest, Set.of(
                    TokenType.Throws
            )),
            Map.entry(NonTerminals.MultiplicativeExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.NewArrayExpression, Set.of(
                    TokenType.New
            )),
            Map.entry(NonTerminals.NewObjectExpression, Set.of(
                    TokenType.New
            )),
            Map.entry(NonTerminals.Parameter, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonTerminals.Parameters, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonTerminals.PostfixExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Null,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.PostfixOp, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.PrimaryExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Null,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.Program, Set.of(
                    TokenType.Class
            )),
            Map.entry(NonTerminals.RelationalExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.ReturnStatement, Set.of(
                    TokenType.Return
            )),
            Map.entry(NonTerminals.S, Set.of(
                    TokenType.Class,
                    TokenType.EOF
            )),
            Map.entry(NonTerminals.Statement, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.While
            )),
            Map.entry(NonTerminals.Type, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonTerminals.UnaryExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonTerminals.WhileStatement, Set.of(
                    TokenType.While
            ))
    );
    public static final Map<NonTerminals, Set<TokenType>> FOLLOW = Map.ofEntries(
            Map.entry(NonTerminals.AdditiveExpression, Set.of(
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.NotEquals,
                    TokenType.Or
            )),
            Map.entry(NonTerminals.Arguments, Set.of(
                    TokenType.RightParen
            )),
            Map.entry(NonTerminals.ArrayAccess, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Divide,
                    TokenType.Dot,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LeftSquareBracket,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.Modulo,
                    TokenType.Multiply,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.AssignmentExpression, Set.of(
                    TokenType.Comma,
                    TokenType.RightParen,
                    TokenType.RightSquareBracket,
                    TokenType.SemiColon
            )),
            Map.entry(NonTerminals.BasicType, Set.of(
                    TokenType.Identifier,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.Block, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Public,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.BlockStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.ClassDeclaration, Set.of(
                    TokenType.Class,
                    TokenType.EOF
            )),
            Map.entry(NonTerminals.ClassMember, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonTerminals.EmptyStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.EqualityExpression, Set.of(
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Or
            )),
            Map.entry(NonTerminals.Expression, Set.of(
                    TokenType.Comma,
                    TokenType.RightParen,
                    TokenType.RightSquareBracket,
                    TokenType.SemiColon
            )),
            Map.entry(NonTerminals.ExpressionStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.Field, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonTerminals.FieldAccess, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Divide,
                    TokenType.Dot,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LeftSquareBracket,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.Modulo,
                    TokenType.Multiply,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.IfStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.LocalVariableDeclarationStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.LogicalAndExpression, Set.of(
                    TokenType.Assign,
                    TokenType.Or
            )),
            Map.entry(NonTerminals.LogicalOrExpression, Set.of(
                    TokenType.Assign
            )),
            Map.entry(NonTerminals.MainMethod, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonTerminals.Method, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonTerminals.MethodInvocation, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Divide,
                    TokenType.Dot,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LeftSquareBracket,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.Modulo,
                    TokenType.Multiply,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.MethodRest, Set.of(
                    TokenType.LeftCurlyBracket
            )),
            Map.entry(NonTerminals.MultiplicativeExpression, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.NewArrayExpression, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.NewObjectExpression, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.Parameter, Set.of(
                    TokenType.Comma
            )),
            Map.entry(NonTerminals.Parameters, Set.of(
                    TokenType.Comma,
                    TokenType.RightParen
            )),
            Map.entry(NonTerminals.PostfixExpression, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Divide,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.Modulo,
                    TokenType.Multiply,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.PostfixOp, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Divide,
                    TokenType.Dot,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LeftSquareBracket,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.Modulo,
                    TokenType.Multiply,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.PrimaryExpression, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.Program, Set.of(
                    TokenType.EOF
            )),
            Map.entry(NonTerminals.RelationalExpression, Set.of(
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Equals,
                    TokenType.NotEquals,
                    TokenType.Or
            )),
            Map.entry(NonTerminals.ReturnStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.S, Set.of(

            )),
            Map.entry(NonTerminals.Statement, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            )),
            Map.entry(NonTerminals.Type, Set.of(
                    TokenType.Identifier,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonTerminals.UnaryExpression, Set.of(
                    TokenType.Add,
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Divide,
                    TokenType.Equals,
                    TokenType.GreaterThan,
                    TokenType.GreaterThanOrEquals,
                    TokenType.LessThan,
                    TokenType.LessThanOrEquals,
                    TokenType.Modulo,
                    TokenType.Multiply,
                    TokenType.NotEquals,
                    TokenType.Or,
                    TokenType.Subtract
            )),
            Map.entry(NonTerminals.WhileStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Else,
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.If,
                    TokenType.Int,
                    TokenType.LeftCurlyBracket,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Not,
                    TokenType.Null,
                    TokenType.Return,
                    TokenType.RightCurlyBracket,
                    TokenType.SemiColon,
                    TokenType.Subtract,
                    TokenType.This,
                    TokenType.True,
                    TokenType.Void,
                    TokenType.While
            ))
    );
}

