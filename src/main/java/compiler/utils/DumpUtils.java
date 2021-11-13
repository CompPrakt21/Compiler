package compiler.utils;

import compiler.ast.AstNode;
import compiler.ast.Identifier;
import compiler.ast.Reference;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class DumpUtils {

    private ArrayList<Integer> used = new ArrayList<>();

    public void dotWriter(AstNode node) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("astDump.dot")))) {
            out.write("digraph {");
            out.newLine();
            recursiveWriter(out, node);
            out.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int recursiveWriter(BufferedWriter out, AstNode node) throws IOException {
        if (node == null) {
            String line = node.getID() + " [label=NULL color=red]\n";
            return node.getID();
        }
        List<AstNode> children = node.getChildren();
        used.add(node.getID());

        String line = node.getID() + " [label=\"" + node.getClass().getSimpleName() + "\n" + node.getName() + "\n";

        line += node.getSpan() + "\n";

        line += "\"";

        if (node.isError()) {
            line += " color=red";
        }
        line += "]\n";

        /*if (node instanceof Reference && !node.isError() && ((Reference) node).getReference() != null) {
            line += ((Reference) node).getReference().getId() + " -> " + node.getId() + " [style=dashed color=blue dir=back]\n";
        }*/

        if (children != null) {
            for (AstNode child : children) {
                if (child == null) {
                    continue;
                }
                if (!used.contains(child.getID())) {
                    line += node.getID() + " -> " + recursiveWriter(out, child) + "\n";
                } else {
                    line += node.getID() + " -> " + child.getID() + " [style=dashed color=blue]\n";
                }

            }
        }
        out.write(line);
        out.newLine();
        return node.getID();
    }
}
