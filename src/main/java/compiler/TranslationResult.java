package compiler;

import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import firm.Graph;
import firm.nodes.Call;

import java.util.Map;

public record TranslationResult(
        Map<Call, MethodDefinition> methodReferences,
        Map<DefinedMethod, Graph> methodGraphs
) { }
