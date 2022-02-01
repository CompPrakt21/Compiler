package compiler;

import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.types.Ty;
import firm.Graph;
import firm.nodes.Call;
import firm.nodes.Node;

import java.util.Map;

public record TranslationResult(
        Map<Call, MethodDefinition> methodReferences,
        Map<DefinedMethod, Graph> methodGraphs,
        Map<Node, Ty> nodeAstTypes
) { }
