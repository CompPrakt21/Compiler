package compiler;

import java.util.Map;
import java.util.Set;

public class Grammar {

    public enum NonT {
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
        WhileStatement;

        public boolean firstContains(TokenType type) {
            return FIRST.get(this).contains(type);
        }

        public boolean followContains(TokenType type) {
            return FOLLOW.get(this).contains(type);
        }
    }


    public static final Map<NonT, Set<TokenType>> FIRST = Map.ofEntries(

            Map.entry(NonT.AdditiveExpression, Set.of(
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
            Map.entry(NonT.Arguments, Set.of(

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
            Map.entry(NonT.ArrayAccess, Set.of(
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonT.AssignmentExpression, Set.of(
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
            Map.entry(NonT.BasicType, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonT.Block, Set.of(
                    TokenType.LeftCurlyBracket
            )),
            Map.entry(NonT.BlockStatement, Set.of(
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
            Map.entry(NonT.ClassDeclaration, Set.of(
                    TokenType.Class
            )),
            Map.entry(NonT.ClassMember, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonT.EmptyStatement, Set.of(
                    TokenType.SemiColon
            )),
            Map.entry(NonT.EqualityExpression, Set.of(
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
            Map.entry(NonT.Expression, Set.of(
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
            Map.entry(NonT.ExpressionStatement, Set.of(
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
            Map.entry(NonT.Field, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonT.FieldAccess, Set.of(
                    TokenType.Dot
            )),
            Map.entry(NonT.IfStatement, Set.of(
                    TokenType.If
            )),
            Map.entry(NonT.LocalVariableDeclarationStatement, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonT.LogicalAndExpression, Set.of(
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
            Map.entry(NonT.LogicalOrExpression, Set.of(
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
            Map.entry(NonT.MainMethod, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonT.Method, Set.of(
                    TokenType.Public
            )),
            Map.entry(NonT.MethodInvocation, Set.of(
                    TokenType.Dot
            )),
            Map.entry(NonT.MethodRest, Set.of(
                    TokenType.Throws
            )),
            Map.entry(NonT.MultiplicativeExpression, Set.of(
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
            Map.entry(NonT.NewArrayExpression, Set.of(
                    TokenType.New
            )),
            Map.entry(NonT.NewObjectExpression, Set.of(
                    TokenType.New
            )),
            Map.entry(NonT.Parameter, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonT.Parameters, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonT.PostfixExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Null,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonT.PostfixOp, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonT.PrimaryExpression, Set.of(
                    TokenType.False,
                    TokenType.Identifier,
                    TokenType.IntLiteral,
                    TokenType.LeftParen,
                    TokenType.New,
                    TokenType.Null,
                    TokenType.This,
                    TokenType.True
            )),
            Map.entry(NonT.Program, Set.of(
                    TokenType.Class
            )),
            Map.entry(NonT.RelationalExpression, Set.of(
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
            Map.entry(NonT.ReturnStatement, Set.of(
                    TokenType.Return
            )),
            Map.entry(NonT.S, Set.of(
                    TokenType.Class,
                    TokenType.EOF
            )),
            Map.entry(NonT.Statement, Set.of(
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
            Map.entry(NonT.Type, Set.of(
                    TokenType.Boolean,
                    TokenType.Identifier,
                    TokenType.Int,
                    TokenType.Void
            )),
            Map.entry(NonT.UnaryExpression, Set.of(
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
            Map.entry(NonT.WhileStatement, Set.of(
                    TokenType.While
            ))
    );
    public static final Map<NonT, Set<TokenType>> FOLLOW = Map.ofEntries(
            Map.entry(NonT.AdditiveExpression, Set.of(
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
            Map.entry(NonT.Arguments, Set.of(
                    TokenType.RightParen
            )),
            Map.entry(NonT.ArrayAccess, Set.of(
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
            Map.entry(NonT.AssignmentExpression, Set.of(
                    TokenType.Comma,
                    TokenType.RightParen,
                    TokenType.RightSquareBracket,
                    TokenType.SemiColon
            )),
            Map.entry(NonT.BasicType, Set.of(
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonT.Block, Set.of(
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
            Map.entry(NonT.BlockStatement, Set.of(
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
            Map.entry(NonT.ClassDeclaration, Set.of(
                    TokenType.Class,
                    TokenType.EOF
            )),
            Map.entry(NonT.ClassMember, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonT.EmptyStatement, Set.of(
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
            Map.entry(NonT.EqualityExpression, Set.of(
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Or
            )),
            Map.entry(NonT.Expression, Set.of(
                    TokenType.Comma,
                    TokenType.RightParen,
                    TokenType.RightSquareBracket,
                    TokenType.SemiColon
            )),
            Map.entry(NonT.ExpressionStatement, Set.of(
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
            Map.entry(NonT.Field, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonT.FieldAccess, Set.of(
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
            Map.entry(NonT.IfStatement, Set.of(
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
            Map.entry(NonT.LocalVariableDeclarationStatement, Set.of(
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
            Map.entry(NonT.LogicalAndExpression, Set.of(
                    TokenType.Assign,
                    TokenType.Or
            )),
            Map.entry(NonT.LogicalOrExpression, Set.of(
                    TokenType.Assign
            )),
            Map.entry(NonT.MainMethod, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonT.Method, Set.of(
                    TokenType.Public,
                    TokenType.RightCurlyBracket
            )),
            Map.entry(NonT.MethodInvocation, Set.of(
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
            Map.entry(NonT.MethodRest, Set.of(
                    TokenType.LeftCurlyBracket
            )),
            Map.entry(NonT.MultiplicativeExpression, Set.of(
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
            Map.entry(NonT.NewArrayExpression, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonT.NewObjectExpression, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonT.Parameter, Set.of(
                    TokenType.Comma
            )),
            Map.entry(NonT.Parameters, Set.of(
                    TokenType.Comma,
                    TokenType.RightParen
            )),
            Map.entry(NonT.PostfixExpression, Set.of(
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
            Map.entry(NonT.PostfixOp, Set.of(
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
            Map.entry(NonT.PrimaryExpression, Set.of(
                    TokenType.Dot,
                    TokenType.LeftSquareBracket
            )),
            Map.entry(NonT.Program, Set.of(
                    TokenType.EOF
            )),
            Map.entry(NonT.RelationalExpression, Set.of(
                    TokenType.And,
                    TokenType.Assign,
                    TokenType.Equals,
                    TokenType.NotEquals,
                    TokenType.Or
            )),
            Map.entry(NonT.ReturnStatement, Set.of(
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
            Map.entry(NonT.S, Set.of(

            )),
            Map.entry(NonT.Statement, Set.of(
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
            Map.entry(NonT.Type, Set.of(
                    TokenType.Identifier
            )),
            Map.entry(NonT.UnaryExpression, Set.of(
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
            Map.entry(NonT.WhileStatement, Set.of(
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

