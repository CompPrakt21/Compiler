package compiler;

import compiler.semantic.resolution.SymbolTable;
import org.junit.jupiter.api.Test;

public class SymbolTableTest {
    @Test
    public void symbolTableTest() {
        var symTable = new SymbolTable<Integer>();

        symTable.insert("a", 1);
        symTable.insert("b", 2);
        symTable.insert("c", 3);

        System.out.println(symTable);

        symTable.enterScope();

        symTable.insert("b", 100);
        symTable.insert("d", 4);

        System.out.println(symTable);

        symTable.leaveScope();

        System.out.println(symTable);
    }
}
