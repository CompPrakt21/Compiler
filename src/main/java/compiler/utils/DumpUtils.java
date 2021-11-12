package compiler.utils;

import compiler.ast.AstNode;
import compiler.ast.Reference;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class DumpUtils {

    public void dotWriter(AstNode node) {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("astDump.dot")))) {
            out.write("digraph {");
            out.newLine();
            recursiveWriter(out, node, "a");
            out.write("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String recursiveWriter(BufferedWriter out, AstNode node, String runthrough) throws IOException {
        if (node == null) {
            String line = runthrough + " [label=NULL color=red]\n";
            return runthrough;
        }
        List<AstNode> children = node.getChildren();

        node.setId(runthrough);

        String line = node.getId() + " [label=\"" + node.getClass().getSimpleName() + "\n" + node.getName() + "\n";

        line += node.getSpan() + "\n";

        line += "\"";

        if (node.isError()) {
            line += " color=red";
        }
        line += "]\n";

        if (node instanceof Reference && !node.isError() && ((Reference) node).getReference() != null) {
            line += ((Reference) node).getReference().getId() + " -> " + node.getId() + " [style=dashed color=blue dir=back]\n";
        }

        if (children != null) {
            int counter = 0;
            for (AstNode child : children) {
                if (child == null) {
                    line += runthrough + "a" + counter + " [label=NULL color=red]\n";
                    counter++;
                    continue;
                }
                if (child.getId() == null) {
                    line += node.getId() + " -> " + recursiveWriter(out, child, runthrough + "a" + counter) + "\n";
                } else {
                    line += node.getId() + " -> " + child.getId() + " [style=dashed color=blue]\n";
                }
                counter++;

            }
        }
        out.write(line);
        out.newLine();
        return node.getId();
    }
}
