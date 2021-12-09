package compiler;

import compiler.codegen.*;
import firm.nodes.Add;
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

        var c = new MovImmediateInstruction(startBlock, generator.nextRegister(), 123);
        var add = new AddInstruction(startBlock, generator.nextRegister(), startBlock.getInputNodes().get(0), c);

        var outReg = generator.nextRegister();
        var add2 = new AddInstruction(startBlock, outReg, add, c);
        startBlock.addOutput(add2);


        var bb1 = new BasicBlock("bb0");
        var jmp = new JumpInstruction(startBlock, bb1);
        startBlock.finish(jmp);

        var in = bb1.addInput(outReg);
        var add3 = new AddInstruction(bb1, generator.nextRegister(), in, in);

        var ret = new ReturnInstruction(bb1, add3);

        add3.setScheduleNext(ret);

        bb1.finish(ret);

        new DumpLlir(new PrintWriter(new File("bb_out.dot"))).dump(ir);
    }
}
