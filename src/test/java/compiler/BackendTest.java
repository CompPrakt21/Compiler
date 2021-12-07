package compiler;

import compiler.codegen.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class BackendTest {
    @Test
    public void backendTest() throws FileNotFoundException {
        var generator = new VirtualRegister.Generator();
        var l = List.of(generator.nextRegister());
        var ir = new LlirGraph(l);

        var startBlock = ir.getStartBlock();

        var c = new MovImmediateInstruction(generator.nextRegister(), 123);
        var add = new AddInstruction(generator.nextRegister(), startBlock.getInputNodes().get(0), c);

        var ret = new ReturnInstruction(add);

        startBlock.finish(ret);

        new DumpLlir(new PrintWriter(new File("bb_out.dot"))).dump(startBlock);
    }
}
