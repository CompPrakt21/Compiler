package compiler.codegen;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.nodes.LlirNode;

import java.util.List;
import java.util.Map;

public record ScheduleResult(
    Map<BasicBlock, List<LlirNode>> schedule
){ }
