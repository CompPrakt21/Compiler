package compiler;

import compiler.ast.Program;
import compiler.semantic.resolution.DefinedClass;
import compiler.semantic.resolution.IntrinsicClass;
import compiler.semantic.resolution.MethodDefinition;
import compiler.semantic.resolution.NameResolution;
import compiler.types.*;
import firm.*;
import firm.Type;
import firm.nodes.Block;
import firm.nodes.Node;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Translation {

    private final Map<Ty, Type> primitiveTypes;
    private final Map<String, PointerType> classTypes;
    private final CompoundType globalType;

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
        var paramTypes = Stream.concat(
                Stream.of(classTypes.get(method.getContainingClass().get().getName())),
                method.getParameterTy().stream().map(ty -> getFirmType((Ty) ty))
        ).toArray(Type[]::new);

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
                    case DefinedClass ignored -> getClassType(clsTy.toString());
                    case IntrinsicClass ignored -> getClassType("String");
                };
                var clsPtr = new PointerType(firmType);
                classTypes.put(clsTy.toString(), clsPtr);
                yield clsPtr;
            }
            case NullTy ignored -> throw new UnsupportedOperationException("void");
        };
        return result;
    }

    private Graph genGraphForMethod(MethodDefinition method) {
        var name = method.getName();
        var type = getMethodType(method);

        Entity methodEnt = new Entity(globalType, name, type);
        if (!"init".equals(name)) {
            Graph graph = new Graph(methodEnt, 10);
            return graph;
        }

        Graph graph = new Graph(methodEnt, 10);

        Construction construction = new Construction(graph);

        Node startNode = construction.newStart();
        Node memProj = construction.newProj(startNode, Mode.getM(), 0);
        //Node argProj = construction.newProj(startNode, Mode.getT(), 1);
        //graph.keepAlive(argProj);
        Node constNode = construction.newConst(0, Mode.getIs());
        Node returnNode = construction.newReturn(memProj, new Node[]{constNode});
        Block endBlock = construction.getGraph().getEndBlock();
        endBlock.addPred(returnNode);
        
        construction.finish();

        return graph;
    }

    public void translate(Program ast, NameResolution.NameResolutionResult nres) {
        // TODO: Fill classTypes

        for (var classTy : nres.classes()) {
            CompoundType classType = (CompoundType) ((PointerType) getFirmType(classTy)).getPointsTo();

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
