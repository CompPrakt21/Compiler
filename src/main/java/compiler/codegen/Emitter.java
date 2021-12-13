package compiler.codegen;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.ControlFlowNode;
import compiler.codegen.llir.LlirNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class Emitter {

    protected StringBuilder builder;

    public Emitter() {
        this.builder = new StringBuilder();
    }

    public void append(String string) {
        this.builder.append(string);
        this.builder.append("\n");
    }

    public abstract void beginBlock(BasicBlock block);
    public abstract void endBlock(ControlFlowNode endNode);

    /* TODO: Add function information */
    public abstract void beginFunction(String name, int numArgs, boolean isVoid);
    public abstract void endFunction();

    public abstract void emitInstruction(LlirNode node);

    public void write(File asmFile) throws IOException {
        Files.write(asmFile.toPath(), List.of(builder.toString()));
    }
}
