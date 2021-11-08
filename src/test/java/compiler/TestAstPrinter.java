package compiler;

import compiler.ast.Program;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAstPrinter {

    @Test
    public void testExampleInput() {
        String in = """
                class HelloWorld
                {
                public int c;
                public boolean[] array;
                public static /* blabla */ void main(String[] args)
                { System.out.println( (43110 + 0) );
                boolean b = true && (!false);
                if (23+19 == (42+0)*1)
                b = (0 < 1);
                else if (!array[2+2]) {
                int x = 0;;
                x = x+1;
                } else {
                new HelloWorld().bar(42+0*1, -1);
                }
                }
                public int bar(int a, int b) { return c = (a+b); }
                }
                """;
        String expected = """
                class HelloWorld {
                \tpublic int bar(int a, int b) {
                \t\treturn c = (a + b);
                \t}
                \tpublic static void main(String[] args) {
                \t\t(System.out).println(43110 + 0);
                \t\tboolean b = true && (!false);
                \t\tif ((23 + 19) == ((42 + 0) * 1))
                \t\t\tb = (0 < 1);
                \t\telse if (!(array[2 + 2])) {
                \t\t\tint x = 0;
                \t\t\tx = (x + 1);
                \t\t} else {
                \t\t\t(new HelloWorld()).bar(42 + (0 * 1), -1);
                \t\t}
                \t}
                \tpublic boolean[] array;
                \tpublic int c;
                }""";
        Parser parser = new Parser(new Lexer(in));
        Program p = parser.parse();
        assertTrue(parser.successfulParse);
        String pretty = AstPrinter.prettyPrint(p);
        assertEquals(expected, pretty);
    }

}
