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
        var ir = new LlirGraph(l, generator);

        var startBlock = ir.getStartBlock();

        var c1 = new MovImmediateInstruction(startBlock, 10);
        var c2 = new MovImmediateInstruction(startBlock, 20);
        var c3 = new MovImmediateInstruction(startBlock, 30);
        var a1 = new AddInstruction(c1, c2);
        var a2 = new AddInstruction(a1, c3);
        var add = new AddInstruction(startBlock.getInputNodes().get(0), a2);

        var outReg = generator.nextRegister();
        var add2 = new AddInstruction(add, a1);
        startBlock.addOutput(add2);


        var bb1 = ir.newBasicBlock();
        var jmp = new JumpInstruction(startBlock, bb1);
        startBlock.finish(jmp);

        var in = bb1.addInput(outReg);
        var add3 = new AddInstruction(in, in);

        var ret = new ReturnInstruction(add3);

        add3.setScheduleNext(ret);

        bb1.finish(ret);

        new DumpLlir(new PrintWriter(new File("bb_out.dot"))).dump(ir);
    }
}
