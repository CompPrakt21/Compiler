package compiler;

import compiler.syntax.Lexer;
import compiler.syntax.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static compiler.syntax.TokenType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLexer {
    private static List<Token> lexAll(String input) {
        Lexer l = new Lexer(input);
        List<Token> result = new ArrayList<>();
        Token t;
        do {
            t = l.nextToken();
            result.add(t);
        } while (t.type != EOF);
        return result;
    }

    private static List<String> formatLexed(String input) {
        return lexAll(input).stream().map(t -> {
            String suffix = switch (t.type) {
                case Identifier -> " " + t.getIdentContent();
                case IntLiteral -> " " + t.getIntLiteralContent();
                case Error -> " `" + input.substring(t.getSpan().start(), t.getSpan().start() + t.getSpan().length()) + "`";
                default -> "";
            };
            return t.type.repr + suffix;
        }).collect(Collectors.toList());
    }

    private static List<String> formatLexedWithSpans(String input) {
        return lexAll(input).stream().map(t -> {
            String suffix = switch (t.type) {
                case Identifier -> " " + t.getIdentContent();
                case IntLiteral -> " " + t.getIntLiteralContent();
                default -> "";
            };
            String spanContent = switch (t.type) {
                case EOF -> "";
                default -> " [`" + input.substring(t.getSpan().start(), t.getSpan().start() + t.getSpan().length()) + "`]";
            };
            return t.type.repr + suffix + spanContent;
        }).collect(Collectors.toList());
    }

    private static final String empty = """
            """;
    private static final List<String> expectedEmpty = Arrays.asList("<EOF>");

    private static final String allTokens = """
            abstract
            assert
            boolean
            break
            byte
            case
            catch
            char
            class
            const
            continue
            default
            double
            do
            else
            enum
            extends
            false
            finally
            final
            float
            for
            goto
            if
            implements
            import
            instanceof
            interface
            int
            long
            native
            new
            null
            package
            private
            protected
            public
            return
            short
            static
            strictfp
            super
            switch
            synchronized
            this
            throws
            throw
            transient
            true
            try
            void
            volatile
            while
            !=
            !
            (
            )
            *=
            *
            ++
            +=
            +
            ,
            -=
            --
            -
            .
            /=
            /
            :
            ;
            <<=
            <<
            <=
            <
            ==
            =
            >=
            >>=
            >>>=
            >>>
            >>
            >
            ?
            %=
            %
            &=
            &&
            &
            [
            ]
            ^=
            ^
            {
            }
            ~
            |=
            ||
            |
            abcdef
            0
            1""";
    private static final List<String> expectedAllTokens = Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "double", "do", "else", "enum", "extends", "false",
            "finally", "final", "float", "for", "goto", "if", "implements", "import", "instanceof", "interface", "int",
            "long", "native", "new", "null", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throws", "throw", "transient", "true", "try",
            "void", "volatile", "while", "!=", "!", "(", ")", "*=", "*", "++", "+=", "+", ",", "-=", "--", "-", ".",
            "/=", "/", ":", ";", "<<=", "<<", "<=", "<", "==", "=", ">=", ">>=", ">>>=", ">>>", ">>", ">", "?", "%=",
            "%", "&=", "&&", "&", "[", "]", "^=", "^", "{", "}", "~", "|=", "||", "|", "<IDENT> abcdef", "<INT> 0",
            "<INT> 1", "<EOF>");

    private static final String whitespace = """
            a y
            z    x
            """;
    private static final List<String> expectedWhitespace = Arrays.asList("<IDENT> a", "<IDENT> y", "<IDENT> z", "<IDENT> x", "<EOF>");

    private static final String comments = """
            /*test*//*123*/ /*
            foobar
            *//*/**/a/**/b/**/""";
    private static final List<String> expectedComments = Arrays.asList("<IDENT> a", "<IDENT> b", "<EOF>");

    private static final String invalidComment = "/*";
    private static final List<String> expectedInvalidComment = Arrays.asList("<Error> `/*`", "<EOF>");

    private static final String identEOF = "abc";
    private static final List<String> expectedIdentEOF = Arrays.asList("<IDENT> abc", "<EOF>");

    private static final String intLiteralEOF = "123";
    private static final List<String> expectedIntLiteralEOF = Arrays.asList("<INT> 123", "<EOF>");

    private static final String keywordEOF = "char";
    private static final List<String> expectedKeywordEOF = Arrays.asList("char", "<EOF>");

    private static final String operatorEOF = "!=";
    private static final List<String> expectedOperatorEOF = Arrays.asList("!=", "<EOF>");

    private static final String idents = """
            fchar charf _ _fOo f123 a=b
            """;
    private static final List<String> expectedIdents = Arrays.asList("<IDENT> fchar", "<IDENT> charf", "<IDENT> _",
            "<IDENT> _fOo", "<IDENT> f123", "<IDENT> a", "=", "<IDENT> b", "<EOF>");

    private static final String intLiterals = """
            00123 4506 07089a 1=2 1char 1char2
            """;
    private static final List<String> expectedIntLiterals = Arrays.asList("<INT> 0", "<INT> 0", "<INT> 123",
            "<INT> 4506", "<INT> 0", "<INT> 7089", "<IDENT> a", "<INT> 1", "=", "<INT> 2", "<INT> 1", "char", "<INT> 1",
            "<IDENT> char2", "<EOF>");

    private static final String keywords = """
            char 1char fchar charf charint char+char
            """;
    private static final List<String> expectedKeywords = Arrays.asList("char", "<INT> 1", "char", "<IDENT> fchar",
            "<IDENT> charf", "<IDENT> charint", "char", "+", "char", "<EOF>");

    private static final String operators = """
            =++= += >>>>>>= === //**// a= 1=
            """;
    private static final List<String> expectedOperators = Arrays.asList("=", "++", "=", "+=", ">>>", ">>>=", "==", "=",
            "/", "/", "<IDENT> a", "=", "<INT> 1", "=", "<EOF>");

    private static final String unknownSymbol = "@";
    private static final List<String> expectedUnknownSymbol = Arrays.asList("<Error> `@`", "<EOF>");

    private static final String invalidUnicodeIdent = "äußerst_falscher_ident";
    private static final List<String> expectedInvalidUnicodeIdent = Arrays.asList("<Error> `ä`", "<IDENT> u",
            "<Error> `ß`", "<IDENT> erst_falscher_ident", "<EOF>");

    private static final String invalidUnicodeIntLiteral = "٨٨٨"; // non-ascii digits
    private static final List<String> expectedInvalidUnicodeIntLiteral = Arrays.asList("<Error> `٨`", "<Error> `٨`",
            "<Error> `٨`", "<EOF>");

    private static final String invalidUnicodeWhitespace = "   ";
    private static final List<String> expectedInvalidUnicodeWhitespace = Arrays.asList("<Error> ` `",
            "<Error> ` `", "<Error> ` `", "<EOF>");

    private static final String basicProgram = """
            /**
             * A classic class
             * @author Beate Best
             */
            class classic {
            	public int method(int arg) {
            		int res = arg+42;
            		res >>= 4;
            	    return res;
            	}
            }
            """;
    private static final List<String> expectedBasicProgram = Arrays.asList("class [`class`]",
            "<IDENT> classic [`classic`]", "{ [`{`]", "public [`public`]", "int [`int`]", "<IDENT> method [`method`]",
            "( [`(`]", "int [`int`]", "<IDENT> arg [`arg`]", ") [`)`]", "{ [`{`]", "int [`int`]", "<IDENT> res [`res`]",
            "= [`=`]", "<IDENT> arg [`arg`]", "+ [`+`]", "<INT> 42 [`42`]", "; [`;`]", "<IDENT> res [`res`]",
            ">>= [`>>=`]", "<INT> 4 [`4`]", "; [`;`]", "return [`return`]", "<IDENT> res [`res`]", "; [`;`]", "} [`}`]",
            "} [`}`]", "<EOF>");


    private static void t(List<String> expected, String input) {
        assertEquals(expected, formatLexed(input));
    }

    @Test
    public void emptyTest() {
        t(expectedEmpty, empty);
    }

    @Test
    public void allTokensTest() {
        t(expectedAllTokens, allTokens);
    }

    @Test
    public void whitespaceTest() {
        t(expectedWhitespace, whitespace);
    }

    @Test
    public void commentsTest() {
        t(expectedComments, comments);
    }

    @Test
    public void invalidCommentTest() {
        t(expectedInvalidComment, invalidComment);
    }

    @Test
    public void identEOFTest() {
        t(expectedIdentEOF, identEOF);
    }

    @Test
    public void intLiteralEOFTest() {
        t(expectedIntLiteralEOF, intLiteralEOF);
    }

    @Test
    public void keywordEOFTest() {
        t(expectedKeywordEOF, keywordEOF);
    }

    @Test
    public void operatorEOFTest() {
        t(expectedOperatorEOF, operatorEOF);
    }

    @Test
    public void identsTest() {
        t(expectedIdents, idents);
    }

    @Test
    public void intLiteralsTest() {
        t(expectedIntLiterals, intLiterals);
    }

    @Test
    public void keywordsTest() {
        t(expectedKeywords, keywords);
    }

    @Test
    public void operatorsTest() {
        t(expectedOperators, operators);
    }

    @Test
    public void unknownSymbolTest() {
        t(expectedUnknownSymbol, unknownSymbol);
    }

    @Test
    public void invalidUnicodeIdentTest() {
        t(expectedInvalidUnicodeIdent, invalidUnicodeIdent);
    }

    @Test
    public void invalidUnicodeIntLiteralTest() {
        t(expectedInvalidUnicodeIntLiteral, invalidUnicodeIntLiteral);
    }

    @Test
    public void invalidUnicodeWhitespaceTest() {
        t(expectedInvalidUnicodeWhitespace, invalidUnicodeWhitespace);
    }

    @Test
    public void basicProgramTest() {
        assertEquals(expectedBasicProgram, formatLexedWithSpans(basicProgram));
    }
}
