package compiler;

import compiler.ast.Program;
import compiler.semantic.AstData;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.Definitions;
import compiler.types.DefinedClassTy;
import compiler.types.TyResult;

import java.io.File;
import java.util.List;

public record FrontendResult(
        File inputFile,
        Program ast,
        Definitions definitions,
        AstData<TyResult> expressionTypes,
        AstData<TyResult> bindingTypes,
        List<DefinedClassTy> classes,
        AstData<Integer> constants,
        AstData<Integer> variableCounts,
        AstData<Boolean> isDeadStatement,
        DefinedMethod mainMethod
) {}
