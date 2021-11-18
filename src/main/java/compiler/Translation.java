package compiler;

import compiler.ast.Program;
import compiler.semantic.resolution.DefinedClass;
import compiler.semantic.resolution.IntrinsicClass;
import compiler.semantic.resolution.MethodDefinition;
import compiler.semantic.resolution.NameResolution;
import compiler.types.*;
import firm.*;
import firm.Type;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Translation {

    // TODO: Think of some better caching
    private Map<Ty, Type> primitiveTypes;
    private Map<String, CompoundType> classTypes;
    private NameResolution.NameResolutionResult nres;
    private CompoundType globalType;

    public Translation() {
        Firm.init();
        this.primitiveTypes = new HashMap<>();
        this.classTypes = new HashMap<>();
        this.globalType = firm.Program.getGlobalType();
    }

    private Type getBoolType() {
        return new PrimitiveType(Mode.getBu());
    }

    private Type getIntType() {
        return new PrimitiveType(Mode.getIs());
    }

    private Type getMethodType(MethodDefinition method) {
        var paramTypes = method.getParameterTy().stream().map(ty -> getFirmType((Ty) ty)).toArray(Type[]::new);
        Type[] returnTypes;
        var returnType = method.getReturnTy();
        returnTypes = switch (returnType) {
            case VoidTy ignored -> new Type[0];
            default -> new Type[]{getFirmType((Ty) returnType)};
        };
        return new MethodType(paramTypes, returnTypes);
    }

    private CompoundType getClassType(String identifier) {
        return new StructType(identifier);
    }

    private Type getArrayType(ArrayTy arrTy) {
        var elemType = getFirmType(arrTy.getChildTy());
        return new firm.PointerType(elemType);
    }

    private Type getFirmType(Ty type) {
        // TODO: Should ArrayTypes also be cached?
        var result = switch (type) {
            case BoolTy ignored -> {
                if (primitiveTypes.containsKey(type)) {
                    yield primitiveTypes.get(type);
                }
                var firmType = getBoolType();
                primitiveTypes.put(type, firmType);
                yield firmType;
            }
            case IntTy ignored -> {
                if (primitiveTypes.containsKey(type)) {
                    yield primitiveTypes.get(type);
                }
                var firmType = getIntType();
                primitiveTypes.put(type, firmType);
                yield firmType;
            }
            case ArrayTy arrTy -> getArrayType(arrTy);
            case ClassTy clsTy -> {
                if (classTypes.containsKey(clsTy.toString())) {
                    yield classTypes.get(clsTy.toString());
                }
                var definition = clsTy.getDefinition();
                var firmType = switch (definition) {
                    case DefinedClass cls -> getClassType(clsTy.toString());
                    case IntrinsicClass ignored -> getClassType("String");
                };
                classTypes.put(clsTy.toString(), firmType);
                yield firmType;
            }
            case NullTy ignored -> throw new UnsupportedOperationException("void");
        };
        return result;
    }

    private Graph genGraphForMethod(MethodDefinition method) {
        var name = method.getName();
        var type = getMethodType(method);

        Entity methodEnt = new Entity(globalType, name, type);

        Graph graph = new Graph(methodEnt, 10);

        Construction construction = new Construction(graph);
        construction.finish();

        return graph;
    }

    public void translate(Program ast, NameResolution.NameResolutionResult nres) {
        this.nres = nres;
        // TODO: Fill classTypes

        for (var classTy : nres.classes()) {
            CompoundType classType = (CompoundType) getFirmType(classTy);

            for (var methodDef : classTy.getDefinition().getMethods().values()) {
                Graph graph = genGraphForMethod(methodDef);
                Dump.dumpGraph(graph, methodDef.getName());
            }
            for (var field : classTy.getDefinition().getFields().values()) {
                Type fieldType = getFirmType((Ty) nres.bindingTypes().get(field).get());
                Entity fieldEnt = new Entity(classType, field.getIdentifier().toString(), fieldType);
            }
        }
        /*
        int numVars = 3;

        Entity methodEnt = new Entity(globalType, "foo", methodType);

        Graph graph = new Graph(methodEnt, 3);
        Construction construction = new Construction(graph);
        Mode intMode = Mode.getIs();
        Node c5 = construction.newConst(5, intMode);
        Node c6 = construction.newConst(6, intMode);
        Node add = construction.newAdd(c5, c6);
        graph.keepAlive(add);

        construction.finish();
        Dump.dumpGraph(graph, "first-dump");*/
        try {
            Dump.dumpTypeGraph("types.vcg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
