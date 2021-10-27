package compiler;

import java.util.Map;

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

		public TokenSet first() {
			return FIRST.get(this);
		}

		public TokenSet follow() {
			return FOLLOW.get(this);
		}

		public boolean firstContains(TokenType type) {
			return FIRST.get(this).contains(type);
		}

		public boolean followContains(TokenType type) {
			return FOLLOW.get(this).contains(type);
		}
	}

	public static final Map<NonT, TokenSet> FIRST = Map.ofEntries(

			Map.entry(NonT.AdditiveExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.Arguments, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.ArrayAccess, TokenSet.of(TokenType.LeftSquareBracket
			)),

			Map.entry(NonT.AssignmentExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.BasicType, TokenSet.of(TokenType.Boolean,
					TokenType.Identifier,
					TokenType.Int,
					TokenType.Void
			)),

			Map.entry(NonT.Block, TokenSet.of(TokenType.LeftCurlyBracket
			)),

			Map.entry(NonT.BlockStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.ClassDeclaration, TokenSet.of(TokenType.Class
			)),

			Map.entry(NonT.ClassMember, TokenSet.of(TokenType.Public
			)),

			Map.entry(NonT.EmptyStatement, TokenSet.of(TokenType.SemiColon
			)),

			Map.entry(NonT.EqualityExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.Expression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.ExpressionStatement, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.Field, TokenSet.of(TokenType.Public
			)),

			Map.entry(NonT.FieldAccess, TokenSet.of(TokenType.Dot
			)),

			Map.entry(NonT.IfStatement, TokenSet.of(TokenType.If
			)),

			Map.entry(NonT.LocalVariableDeclarationStatement, TokenSet.of(TokenType.Boolean,
					TokenType.Identifier,
					TokenType.Int,
					TokenType.Void
			)),

			Map.entry(NonT.LogicalAndExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.LogicalOrExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.MainMethod, TokenSet.of(TokenType.Public
			)),

			Map.entry(NonT.Method, TokenSet.of(TokenType.Public
			)),

			Map.entry(NonT.MethodInvocation, TokenSet.of(TokenType.Dot
			)),

			Map.entry(NonT.MethodRest, TokenSet.of(TokenType.Throws
			)),

			Map.entry(NonT.MultiplicativeExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.NewArrayExpression, TokenSet.of(TokenType.New
			)),

			Map.entry(NonT.NewObjectExpression, TokenSet.of(TokenType.New
			)),

			Map.entry(NonT.Parameter, TokenSet.of(TokenType.Boolean,
					TokenType.Identifier,
					TokenType.Int,
					TokenType.Void
			)),

			Map.entry(NonT.Parameters, TokenSet.of(TokenType.Boolean,
					TokenType.Identifier,
					TokenType.Int,
					TokenType.Void
			)),

			Map.entry(NonT.PostfixExpression, TokenSet.of(TokenType.False,
					TokenType.Identifier,
					TokenType.IntLiteral,
					TokenType.LeftParen,
					TokenType.New,
					TokenType.Null,
					TokenType.This,
					TokenType.True
			)),

			Map.entry(NonT.PostfixOp, TokenSet.of(TokenType.Dot,
					TokenType.LeftSquareBracket
			)),

			Map.entry(NonT.PrimaryExpression, TokenSet.of(TokenType.False,
					TokenType.Identifier,
					TokenType.IntLiteral,
					TokenType.LeftParen,
					TokenType.New,
					TokenType.Null,
					TokenType.This,
					TokenType.True
			)),

			Map.entry(NonT.Program, TokenSet.of(TokenType.Class
			)),

			Map.entry(NonT.RelationalExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.ReturnStatement, TokenSet.of(TokenType.Return
			)),

			Map.entry(NonT.S, TokenSet.of(TokenType.Class,
					TokenType.EOF
			)),

			Map.entry(NonT.Statement, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.Type, TokenSet.of(TokenType.Boolean,
					TokenType.Identifier,
					TokenType.Int,
					TokenType.Void
			)),

			Map.entry(NonT.UnaryExpression, TokenSet.of(TokenType.False,
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

			Map.entry(NonT.WhileStatement, TokenSet.of(TokenType.While
			))

	);
	public static final Map<NonT, TokenSet> FOLLOW = Map.ofEntries(

			Map.entry(NonT.AdditiveExpression, TokenSet.of(TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
					TokenType.Equals,
					TokenType.GreaterThan,
					TokenType.GreaterThanOrEquals,
					TokenType.LessThan,
					TokenType.LessThanOrEquals,
					TokenType.NotEquals,
					TokenType.Or,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.Arguments, TokenSet.of(TokenType.RightParen
			)),

			Map.entry(NonT.ArrayAccess, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.AssignmentExpression, TokenSet.of(TokenType.Comma,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.BasicType, TokenSet.of(TokenType.Identifier,
					TokenType.LeftSquareBracket
			)),

			Map.entry(NonT.Block, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.BlockStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.ClassDeclaration, TokenSet.of(TokenType.Class,
					TokenType.EOF
			)),

			Map.entry(NonT.ClassMember, TokenSet.of(TokenType.Public,
					TokenType.RightCurlyBracket
			)),

			Map.entry(NonT.EmptyStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.EqualityExpression, TokenSet.of(TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
					TokenType.Or,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.Expression, TokenSet.of(TokenType.Comma,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.ExpressionStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.Field, TokenSet.of(TokenType.Public,
					TokenType.RightCurlyBracket
			)),

			Map.entry(NonT.FieldAccess, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.IfStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.LocalVariableDeclarationStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.LogicalAndExpression, TokenSet.of(TokenType.Assign,
					TokenType.Comma,
					TokenType.Or,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.LogicalOrExpression, TokenSet.of(TokenType.Assign,
					TokenType.Comma,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.MainMethod, TokenSet.of(TokenType.Public,
					TokenType.RightCurlyBracket
			)),

			Map.entry(NonT.Method, TokenSet.of(TokenType.Public,
					TokenType.RightCurlyBracket
			)),

			Map.entry(NonT.MethodInvocation, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.MethodRest, TokenSet.of(TokenType.LeftCurlyBracket
			)),

			Map.entry(NonT.MultiplicativeExpression, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
					TokenType.Equals,
					TokenType.GreaterThan,
					TokenType.GreaterThanOrEquals,
					TokenType.LessThan,
					TokenType.LessThanOrEquals,
					TokenType.NotEquals,
					TokenType.Or,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.NewArrayExpression, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.NewObjectExpression, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.Parameter, TokenSet.of(TokenType.Comma,
					TokenType.RightParen
			)),

			Map.entry(NonT.Parameters, TokenSet.of(TokenType.Comma,
					TokenType.RightParen
			)),

			Map.entry(NonT.PostfixExpression, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.PostfixOp, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.PrimaryExpression, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.Program, TokenSet.of(TokenType.EOF
			)),

			Map.entry(NonT.RelationalExpression, TokenSet.of(TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
					TokenType.Equals,
					TokenType.NotEquals,
					TokenType.Or,
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon
			)),

			Map.entry(NonT.ReturnStatement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.S, TokenSet.of(
			)),

			Map.entry(NonT.Statement, TokenSet.of(TokenType.Boolean,
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

			Map.entry(NonT.Type, TokenSet.of(TokenType.Identifier
			)),

			Map.entry(NonT.UnaryExpression, TokenSet.of(TokenType.Add,
					TokenType.And,
					TokenType.Assign,
					TokenType.Comma,
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
					TokenType.RightParen,
					TokenType.RightSquareBracket,
					TokenType.SemiColon,
					TokenType.Subtract
			)),

			Map.entry(NonT.WhileStatement, TokenSet.of(TokenType.Boolean,
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

