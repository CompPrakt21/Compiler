package compiler.codegen;

import compiler.codegen.sir.SirGraph;

public class NaiveRegisterAllocator {

    private SirGraph graph;

    public NaiveRegisterAllocator(SirGraph graph) {
        this.graph = graph;
    }

    /**
     * Every virtual register is replaced with a hardware register.
     * Mutates the graph and schedule if necessary.
     */
    public void allocate() {


    }
}
