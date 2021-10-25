package compiler;

import java.util.Arrays;
import java.util.List;

public enum TokenType {
    // Meta tokens
    Error("<Error>"),
    EOF("<EOF>"),

    // Used keywords
    Boolean("boolean"),
    Class("class"),
    Else("else"),
    False("false"),
    If("if"),
    Int("int"),
    New("new"),
    Null("null"),
    Public("public"),
    Return("return"),
    Static("static"),
    This("this"),
    Throws("throws"),
    True("true"),
    Void("void"),
    While("while"),

    // Used Operators
    NotEquals("!="),
    Not("!"),
    LeftParen("("),
    RightParen(")"),
    Multiply("*"),
    Add("+"),
    Comma(","),
    Subtract("-"),
    Dot("."),
    Divide("/"),
    SemiColon(";"),
    LessThanOrEquals("<="),
    LessThan("<"),
    Equals("=="),
    Assign("="),
    GreaterThanOrEquals(">="),
    GreaterThan(">"),
    Modulo("%"),
    And("&&"),
    LeftSquareBracket("["),
    RightSquareBracket("]"),
    LeftCurlyBracket("{"),
    RightCurlyBracket("}"),
    Or("||"),

    // Tokens with content
    Identifier("<IDENT>"),
    IntLiteral("<INT>"),

    // Unused keywords
    Abstract("abstract"),
    Assert("assert"),
    Break("break"),
    Byte("byte"),
    Case("case"),
    Catch("catch"),
    Char("char"),
    Const("const"),
    Continue("continue"),
    Default("default"),
    Double("double"),
    Do("do"),
    Enum("enum"),
    Extends("extends"),
    Finally("finally"),
    Final("final"),
    Float("float"),
    For("for"),
    Goto("goto"),
    Implements("implements"),
    Import("import"),
    Instanceof("instanceof"),
    Interface("interface"),
    Long("long"),
    Native("native"),
    Package("package"),
    Private("private"),
    Protected("protected"),
    Short("short"),
    Strictfp("strictfp"),
    Super("super"),
    Switch("switch"),
    Synchronized("synchronized"),
    Transient("transient"),
    Try("try"),
    Volatile("volatile"),

    // Unused operators
    MultiplyAssign("*="),
    Increment("++"),
    AddAssign("+="),
    SubtractAssign("-="),
    Decrement("--"),
    DivideAssign("/="),
    Colon(":"),
    BitwiseArithmeticShiftLeftAssign("<<="),
    BitwiseArithmeticShiftLeft("<<"),
    BitwiseArithmeticShiftRightAssign(">>="),
    BitwiseLogicalShiftRight(">>>"),
    BitwiseArithmeticShiftRight(">>"),
    QuestionMark("?"),
    ModuloAssign("%="),
    BitwiseAndAssign("&="),
    BitwiseAnd("&"),
    BitwiseXorAssign("^="),
    BitwiseXor("^"),
    BitwiseNot("~"),
    BitwiseOrAssign("|="),
    BitwiseOr("|");

    public static final List<TokenType> KEYWORDS = Arrays.asList(
            Boolean, Class, Else, False, If, Int, New, Null, Public,
            Return, Static, This, Throws, True, Void, While,
            Abstract, Assert, Break, Byte, Case, Catch, Char, Const,
            Continue, Default, Double, Do, Enum, Extends, Finally, Final, Float,
            For, Goto, Implements, Import, Instanceof, Interface, Long, Native,
            Package, Private, Protected, Short, Strictfp, Super, Switch,
            Synchronized, Transient, Try, Volatile
    );

    public static final List<TokenType> OPERATORS = Arrays.asList(
            NotEquals, Not, LeftParen, RightParen, Multiply, Add, Comma,
            Subtract, Dot, Divide, SemiColon, LessThanOrEquals, LessThan,
            Equals, Assign, GreaterThanOrEquals, GreaterThan, Modulo,
            And, LeftSquareBracket, RightSquareBracket,
            LeftCurlyBracket, RightCurlyBracket, Or,
            MultiplyAssign, Increment, AddAssign, SubtractAssign, Decrement,
            DivideAssign, Colon, BitwiseArithmeticShiftLeftAssign,
            BitwiseArithmeticShiftLeft, BitwiseArithmeticShiftRightAssign,
            BitwiseLogicalShiftRight, BitwiseArithmeticShiftRight,
            QuestionMark, ModuloAssign, BitwiseAndAssign, BitwiseAnd,
            BitwiseXorAssign, BitwiseXor, BitwiseNot, BitwiseOrAssign,
            BitwiseOr);

    public String repr;

    TokenType(String repr) {
        this.repr = repr;
    }
}