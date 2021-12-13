package compiler;

import compiler.codegen.MolkiEmitter;
import compiler.codegen.llir.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

public class MolkiEmitterTest {

    @Test
    public void emitSomeAssembly() throws IOException {
        var generator = new VirtualRegister.Generator();
        var ir = new LlirGraph(generator);

        var startBlock = ir.getStartBlock();

        var memInputStart = new MemoryInputNode(startBlock);

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
}
