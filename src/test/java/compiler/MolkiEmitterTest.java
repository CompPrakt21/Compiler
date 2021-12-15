package compiler;

import compiler.codegen.MolkiEmitter;
import compiler.codegen.llir.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

//@Disabled("These require manual checking and emit files")
public class MolkiEmitterTest {

    @Test
    public void add10To20() throws IOException {
        var generator = new VirtualRegister.Generator();
        var ir = new LlirGraph(generator);
        var startBlock = ir.getStartBlock();

        var c1 = startBlock.newMovImmediate(10);
        var c2 = startBlock.newMovImmediate(20);
        var a1 = startBlock.newAdd(c1, c2);
        var ret = startBlock.newReturn(Optional.of(a1));

        startBlock.finish(ret);

        MolkiEmitter emitter = new MolkiEmitter();

        emitter.beginFunction("minijava_main", 0, false);

        emitter.emitInstruction(c1);
        emitter.emitInstruction(c2);
        emitter.emitInstruction(a1);
        emitter.emitInstruction(ret);

        emitter.write(new File("test.s"));
    }

    @Test
    public void mul10To3() throws IOException {
        var generator = new VirtualRegister.Generator();
        var ir = new LlirGraph(generator);
        var startBlock = ir.getStartBlock();

        var c1 = startBlock.newMovImmediate(10);
        var c2 = startBlock.newMovImmediate(3);
        var a1 = startBlock.newMul(c1, c2);
        var ret = startBlock.newReturn(Optional.of(a1));

        startBlock.finish(ret);

        MolkiEmitter emitter = new MolkiEmitter();

        emitter.beginFunction("minijava_main", 0, false);

        emitter.emitInstruction(c1);
        emitter.emitInstruction(c2);
        emitter.emitInstruction(a1);
        emitter.emitInstruction(ret);

        emitter.write(new File("test.s"));
    }
}
