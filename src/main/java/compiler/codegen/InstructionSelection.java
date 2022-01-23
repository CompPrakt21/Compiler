package compiler.codegen;

import compiler.TranslationResult;
import compiler.codegen.llir.nodes.*;
import compiler.codegen.llir.nodes.Constant;
import compiler.codegen.llir.nodes.MemoryLocation;
import compiler.semantic.resolution.DefinedMethod;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class InstructionSelection extends FirmToLlir {

    public InstructionSelection(DefinedMethod method, Graph firmGraph, TranslationResult translation) {
        super(method, firmGraph, translation);
    }

    /**
     * Sometimes multiple firm nodes can be combined into a single instruction.
     * For example, the instruction add r1 [r2 + r3] combines an Add node with a
     * Load node.
     * In this case the load node can only be folded into a larger Add instruction
     * if it is *only* used by the corresponding Add node and it is in the same
     * basic block.
     *
     * This method assumes that pred is a predecessor of pred.
     */
    private boolean canBeFoldedIntoInstruction(Node n, Node pred) {
        if (pred instanceof Const) return true;

        // Proj nodes might not have been visited, if their parent (for example) has been visited.
        // But in this case we cannot fold them.
        if (pred instanceof Proj proj && !canBeFoldedIntoInstruction(proj, proj.getPred())) return false;

        var hasSingleUse = pred instanceof Load || pred instanceof Store || pred instanceof Call ? BackEdges.getNOuts(pred) == 2 : BackEdges.getNOuts(pred) ==1;
        var sameBB = n.getBlock().equals(pred.getBlock());
        var notVisited = !this.visited.contains(pred);
        return hasSingleUse && sameBB && notVisited;
    }

    private static Proj getMemProj(Node n) {
        assert n instanceof Load || n instanceof Store || n instanceof Call;

        var outs = StreamSupport.stream(BackEdges.getOuts(n).spliterator(), false);

        var memProj = outs.filter(succ -> succ.node instanceof Proj p && p.getMode().equals(Mode.getM())).findFirst();

        return memProj.map(proj -> (Proj) proj.node).orElseThrow(() -> new AssertionError("Load/Store/Call without mem proj node."));
    }

    private record LeftRight(Node left, Node right){}
    private static LeftRight chooseCommutativeBinaryNodeArgumentOrder(Node lhs, Node rhs) {
        Function<Node, Integer> value = node -> switch (node) {
            case Const ignored -> 10;
            case Mul ignored -> 20;
            case Proj proj && proj.getPred() instanceof Load -> 100;
            default -> 0;
        };

        var lhsValue = value.apply(lhs);
        var rhsValue = value.apply(rhs);

        if (lhsValue < rhsValue) {
            return new LeftRight(lhs, rhs);
        } else {
            return new LeftRight(rhs, lhs);
        }
    }

    private static boolean isValidIndexScale(int i) {
        return i == 1 || i == 2 || i == 4 || i == 8;
    }

    private static boolean isConstWithValidIndex(Node n) {
        return n instanceof Const c && isValidIndexScale(c.getTarval().asInt());
    }

    /**
     * Tries to match addr node to the x86 memory location pattern [constant + base + index * scale].
     * Compared to matchMemoryLocation this method checks that addr can actually be folded together with memNode.
     * @return the matched memory location.
     */
    private MemoryLocation matchMemoryLocationArgument(Node memNode, Node addr) {
        if (!canBeFoldedIntoInstruction(memNode, addr)) {
            this.visitNode(addr);
            var llirAddr = (RegisterNode) getPredLlirNode(memNode, addr);
            return MemoryLocation.base(llirAddr);
        }

        return this.matchMemoryLocation(addr);
    }

    /**
     * Tries to match addr node to the x86 memory location pattern [constant + base + index * scale].
     * The callee has to verify that addr can be the root node of a memory pattern.
     * @return the matched memory location.
     */
    private MemoryLocation matchMemoryLocation(Node addr) {
        record Summand(Optional<Node> parent, Node node) {}
        var summands = new ArrayList<Summand>();

        if (addr instanceof Const c) {
            summands.add(new Summand(Optional.empty(), c));

        } else if (addr instanceof Mul mul) {
            summands.add(new Summand(Optional.empty(), mul));

        } else if (addr instanceof Add add) {

            // We can expand either the left or right side, if they are also an Add node.
            // If both are add nodes, we need to decide which is more suitable.

            if (add.getLeft() instanceof Add lAdd && canBeFoldedIntoInstruction(add, lAdd) && add.getRight() instanceof Add rAdd && canBeFoldedIntoInstruction(add, rAdd)) {
                Function<Add, Integer> eval = addArg -> {
                    int points = 0;
                    if (addArg.getRight() instanceof Const || addArg.getLeft() instanceof Const) {
                        points += 10;
                    }

                    Function<Mul, Boolean> isSuitableMul = mul -> canBeFoldedIntoInstruction(add, mul) && (isConstWithValidIndex(mul.getLeft()) || isConstWithValidIndex(mul.getRight()));
                    if (add.getRight() instanceof Mul lMul && isSuitableMul.apply(lMul) || add.getLeft() instanceof Mul rMul && isSuitableMul.apply(rMul)) {
                        points += 10;
                    }
                    return points;
                };

                if (eval.apply(lAdd) < eval.apply(rAdd)) {
                    summands.add(new Summand(Optional.of(add), lAdd));
                    summands.add(new Summand(Optional.of(rAdd), rAdd.getLeft()));
                    summands.add(new Summand(Optional.of(rAdd), rAdd.getRight()));
                } else {
                    summands.add(new Summand(Optional.of(lAdd), lAdd.getLeft()));
                    summands.add(new Summand(Optional.of(lAdd), lAdd.getRight()));
                    summands.add(new Summand(Optional.of(add), rAdd));
                }
            } else if (add.getLeft() instanceof Add lAdd && canBeFoldedIntoInstruction(add, lAdd) && (isConstWithValidIndex(lAdd.getRight()) || isConstWithValidIndex(lAdd.getRight()) || isConstWithValidIndex(add.getRight()))) {
                summands.add(new Summand(Optional.of(lAdd), lAdd.getLeft()));
                summands.add(new Summand(Optional.of(lAdd), lAdd.getRight()));
                summands.add(new Summand(Optional.of(add), add.getRight()));
            } else if (add.getRight() instanceof Add rAdd && canBeFoldedIntoInstruction(add, rAdd) && (isConstWithValidIndex(rAdd.getRight()) || isConstWithValidIndex(rAdd.getRight()) || isConstWithValidIndex(add.getLeft()))) {
                summands.add(new Summand(Optional.of(rAdd), rAdd.getLeft()));
                summands.add(new Summand(Optional.of(rAdd), rAdd.getRight()));
                summands.add(new Summand(Optional.of(add), add.getLeft()));
            } else {
                summands.add(new Summand(Optional.of(add), add.getLeft()));
                summands.add(new Summand(Optional.of(add), add.getRight()));
            }
        } else {
            summands.add(new Summand(Optional.empty(), addr));
        }

        // We now have all summands.
        MemoryLocation loc = new MemoryLocation();
        boolean haveSetConstant = false;
        boolean haveSetBase = false;
        boolean haveSetIndex = false;

        for (var summand : summands) {
            if (summand.node instanceof Const c && !haveSetConstant) {
                loc.setConstant(c.getTarval().asInt());
                haveSetConstant = true;

            } else if (summand.node instanceof Mul mul && isConstWithValidIndex(mul.getLeft()) && !haveSetIndex) {

                var c = (Const) mul.getLeft();
                this.visitNode(mul.getRight());
                var llirMulRight = (RegisterNode) getPredLlirNode(mul, mul.getRight());

                loc.setIndex(llirMulRight);
                loc.setScale(c.getTarval().asInt());

                haveSetIndex = true;

            } else if (summand.node instanceof Mul mul && isConstWithValidIndex(mul.getRight()) && !haveSetIndex) {

                var c = (Const) mul.getRight();
                this.visitNode(mul.getLeft());
                var llirMulLeft = (RegisterNode) getPredLlirNode(mul, mul.getLeft());

                loc.setIndex(llirMulLeft);
                loc.setScale(c.getTarval().asInt());

                haveSetIndex = true;

            } else if (!haveSetBase) {
                this.visitNode(summand.node);

                var llirSummand = summand.parent.isPresent() ?
                        (RegisterNode) getPredLlirNode(summand.parent.get(), summand.node)
                        : (RegisterNode) this.valueNodeMap.get(summand.node);

                loc.setBaseRegister(llirSummand);

                haveSetBase = true;

            } else if (!haveSetIndex) {
                this.visitNode(summand.node);

                var llirSummand = summand.parent.isPresent() ?
                        (RegisterNode) getPredLlirNode(summand.parent.get(), summand.node)
                        : (RegisterNode) this.valueNodeMap.get(summand.node);
                loc.setIndex(llirSummand); // scale is 1 by default

                haveSetIndex = true;

            } else {
                throw new AssertionError("Couldn't fit all summands into the memory location pattern.");
            }
        }

        assert loc.verify();

        return loc;
    }

    private RegisterNode newBinopInstruction(Binop binOp, RegisterNode lhs, SimpleOperand rhs) {
        var bb = getBasicBlock(binOp);
        return switch (binOp) {
            case Add ignored -> bb.newAdd(lhs, rhs);
            case Mul ignored -> bb.newMul(lhs, rhs);
            case Eor ignored -> bb.newXor(lhs, rhs);
            default -> throw new AssertionError("invalid node type");
        };
    }

    /**
     * Matches commutative binary ops (add, mul, xor)
     */
    private void visitBinOp(Binop binOp) {
        var binArgOrder = chooseCommutativeBinaryNodeArgumentOrder(binOp.getLeft(), binOp.getRight());

        this.visitNode(binArgOrder.left);
        var lhs = (RegisterNode)getPredLlirNode(binOp, binArgOrder.left);

        var bb = getBasicBlock(binOp);

        if (binArgOrder.right instanceof Proj proj && canBeFoldedIntoInstruction(binOp, proj)
                && proj.getPred() instanceof Load load
        ) {
            // binOp r1 [*]
            this.visitNode(load.getMem());
            var llirMem = getPredSideEffectNode(load, load.getMem());

            var loc = this.matchMemoryLocationArgument(load, load.getPtr());

            var llirAdd = switch (binOp) {
                case Add ignored -> bb.newAddFromMem(lhs, loc, llirMem);
                case Mul ignored -> bb.newMulFromMem(lhs, loc, llirMem);
                case Eor ignored -> bb.newXorFromMem(lhs, loc, llirMem);
                default -> throw new AssertionError("invalid node type");
            };

            var memProj = getMemProj(load);
            this.visited.add(memProj);

            this.registerLlirNode(binOp, llirAdd);
            this.registerSideEffect(memProj, llirAdd);
        } else if (binArgOrder.right instanceof Const cons) {
            // binOp r1 const

            var llirBinOp = newBinopInstruction(binOp, lhs, new Constant(cons.getTarval().asInt()));
            this.registerLlirNode(binOp, llirBinOp);
        }

        // Nothing matched, use naive binOp r1 r2 instruction.
        if (!this.valueNodeMap.containsKey(binOp)) {
            this.visitNode(binArgOrder.right);
            var rhs = (RegisterNode)getPredLlirNode(binOp, binArgOrder.right);

            var llirBin = newBinopInstruction(binOp, lhs, rhs);
            this.registerLlirNode(binOp, llirBin);
        }
    }

    private void matchSubNode(Node node, Node firmLhs, Node firmRhs) {
        var bb = getBasicBlock(node);

        this.visitNode(firmLhs);
        var lhs = (RegisterNode) getPredLlirNode(node, firmLhs);

        if (firmRhs instanceof Load load && canBeFoldedIntoInstruction(node, load)) {
            var loc = this.matchMemoryLocationArgument(load, load.getPtr());

            this.visitNode(load.getMem());
            var llirMem = getPredSideEffectNode(load, load.getMem());

            var llirSub = bb.newSubFromMem(lhs, loc, llirMem);

            var memProj = getMemProj(load);
            this.visited.add(memProj);

            this.registerLlirNode(node, llirSub);
            this.registerSideEffect(memProj, llirSub);
        } else if (firmRhs instanceof Const c) {
            var llirSub = bb.newSub(lhs, new Constant(c.getTarval().asInt()));
            this.registerLlirNode(node, llirSub);
        } else {
            this.visitNode(firmRhs);
            var rhs = (RegisterNode) getPredLlirNode(node, firmRhs);
            var llirSub = bb.newSub(lhs, rhs);
            this.registerLlirNode(node, llirSub);
        }
    }

    private boolean shouldBeLEA(Add add) {
        var summands = new ArrayList<>();

        if (add.getRight() instanceof Add rAdd) {
            summands.add(rAdd.getLeft());
            summands.add(rAdd.getRight());
        } else {
            summands.add(add.getRight());
        }

        if (add.getLeft() instanceof Add lAdd) {
            summands.add(lAdd.getLeft());
            summands.add(lAdd.getRight());
        } else {
            summands.add(add.getRight());
        }

        var haveConstSummand = false;
        var haveMulSummand = false;
        var haveRegisterSummand = false;
        var haveIndexSummand = false;

        for (var summand : summands) {
            if (!haveConstSummand && summand instanceof Const) {
                haveConstSummand = true;
            } else if (!haveMulSummand && summand instanceof Mul mul && (isConstWithValidIndex(mul.getLeft()) || isConstWithValidIndex(mul.getRight()))) {
                haveMulSummand = true;
                haveIndexSummand = false;
            } else if (!haveRegisterSummand) {
                haveRegisterSummand = true;
            } else if (!haveMulSummand && !haveIndexSummand) {
                haveIndexSummand = true;
            }
        }

        return haveMulSummand || (haveConstSummand && haveRegisterSummand && haveIndexSummand);
    }

    @Override
    public void visit(Add add) {
        // Should this add be lowered to a sub instruction
        var lhsIsMinus = add.getLeft() instanceof Minus minus && canBeFoldedIntoInstruction(add, minus);
        var rhsIsMinus = add.getRight() instanceof Minus minus && canBeFoldedIntoInstruction(add, minus);

        if (lhsIsMinus && rhsIsMinus) {
            var lhsPred = add.getLeft().getPred(0);
            var rhsPred = add.getRight().getPred(0);
            var order = chooseCommutativeBinaryNodeArgumentOrder(lhsPred, rhsPred);
            if (order.left.equals(lhsPred)) {
                // add add.getLeft() -rhsPred
                this.matchSubNode(add, add.getLeft(), rhsPred);
            } else {
                // add add.getRight() -lhsPred
                this.matchSubNode(add, add.getRight(), lhsPred);
            }
            return;
        } else if (lhsIsMinus) {
            this.matchSubNode(add, add.getRight(), add.getLeft().getPred(0));
            return;
        } else if (rhsIsMinus) {
            this.matchSubNode(add, add.getLeft(), add.getRight().getPred(0));
            return;
        }

        if (shouldBeLEA(add)) {
            var loc = this.matchMemoryLocation(add);
            var bb = getBasicBlock(add);
            var llirLea = bb.newLoadEffectiveAddress(modeToRegisterWidth(add.getMode()), loc);
            this.registerLlirNode(add, llirLea);
        } else {
            this.visitBinOp(add);
        }
    }

    @Override
    public void visit(Mul mul) {
        this.visitBinOp(mul);
    }

    @Override
    public void visit(Eor xor) {
        this.visitBinOp(xor);
    }

    @Override
    public void visit(Sub sub) {
        this.matchSubNode(sub, sub.getLeft(), sub.getRight());
    }

    @Override
    public void visit(Cmp cmp) {
        var argOrder = chooseCommutativeBinaryNodeArgumentOrder(cmp.getLeft(), cmp.getRight());
        var reversedArgs = !argOrder.left.equals(cmp.getLeft());

        var bb = getBasicBlock(cmp);

        this.visitNode(argOrder.left);
        var lhs = (RegisterNode) getPredLlirNode(cmp, argOrder.left);

        LlirNode llirCmp;

        if (argOrder.right instanceof Proj proj && canBeFoldedIntoInstruction(cmp, proj) && proj.getPred() instanceof Load load) {
            this.visitNode(load.getMem());
            var llirMem = getPredSideEffectNode(load, load.getMem());
            var loc = this.matchMemoryLocationArgument(load, load.getPtr());

            llirCmp = bb.newCmpFromMem(lhs, loc, reversedArgs, llirMem);

            var memProj = getMemProj(load);
            this.visited.add(memProj);

            this.registerSideEffect(memProj, (SideEffect) llirCmp);
        } else if (argOrder.right instanceof Const c) {
            llirCmp = bb.newCmp(lhs, new Constant(c.getTarval().asInt()), reversedArgs);
        } else {
            this.visitNode(argOrder.right);
            var llirArgRight = (RegisterNode) getPredLlirNode(cmp, argOrder.right);
            llirCmp = bb.newCmp(lhs, llirArgRight, reversedArgs);
        }

        this.registerLlirNode(cmp, llirCmp);
    }

    @Override
    public void visit(Load load) {
        var memLoc = this.matchMemoryLocationArgument(load, load.getPtr());
        this.visitNode(load.getMem());

        var bb = getBasicBlock(load);
        var memNode = getPredSideEffectNode(load, load.getMem());

        Mode outputMode = load.getLoadMode();

        var llirLoad = bb.newMovLoad(memLoc, memNode, modeToRegisterWidth(outputMode));
        registerLlirNode(load, llirLoad);
    }

    public void visit(Store store) {
        var memLoc = this.matchMemoryLocationArgument(store, store.getPtr());
        this.visitNode(store.getValue());
        this.visitNode(store.getMem());

        var bb = getBasicBlock(store);

        var memNode = getPredSideEffectNode(store, store.getMem());
        var valueNode = (RegisterNode)getPredLlirNode(store, store.getValue());

        var llirStore = bb.newMovStore(memLoc, valueNode, memNode, modeToRegisterWidth(store.getValue().getMode()));
        registerLlirNode(store, llirStore);
    }

    @Override
    protected void lower() {
        super.lower();
    }
}
