package compiler;

import compiler.codegen.Register.Width;
import compiler.codegen.Register;
import compiler.codegen.VirtualRegister;
import compiler.codegen.llir.*;
import compiler.codegen.llir.nodes.MemoryInputNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Optional;

public class BackendTest {
    @Test
    public void backendTest() throws FileNotFoundException {
        var generator = new VirtualRegister.Generator();
        var ir = new LlirGraph(generator);

        var startBlock = ir.getStartBlock();

        var memInputStart = new MemoryInputNode(startBlock);

        var c1 = startBlock.newMovImmediate(10, Register.Width.BIT32);
        var c2 = startBlock.newMovImmediate(20, Register.Width.BIT32);
        var c3 = startBlock.newMovImmediate(30, Register.Width.BIT32);
        var a1 = startBlock.newAdd(c1, c2);
        var a2 = startBlock.newAdd(a1, c3);
        var add = startBlock.newAdd(startBlock.getInputNodes().get(0), a2);

        var outReg = generator.nextRegister(Register.Width.BIT32);
        var add2 = startBlock.newAdd(add, a1);
        startBlock.addOutput(add2);

        var bb1 = ir.newBasicBlock();
        var jmp = startBlock.newJump(bb1);
        startBlock.finish(jmp);

        var in = bb1.addInput(outReg);
        var add3 = bb1.newAdd(in, in);

        var memInputBb1 = new MemoryInputNode(bb1);
        var ret = bb1.newReturn(Optional.of(add3));

        //add3.setScheduleNext(ret);

        bb1.finish(ret);

        new DumpLlir(new PrintWriter(new File("bb_out.dot"))).dump(ir);
    }
}
