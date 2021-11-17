package compiler.errors;

import compiler.Span;
import compiler.ast.ReturnStatement;
import compiler.ast.Statement;
import compiler.diagnostics.CompilerWarning;
import compiler.diagnostics.Source;

import java.util.List;

public class DeadCodeWarning extends CompilerWarning {
    private final Span stmt;
    private final List<ReturnStatement> retnStmts;

    public DeadCodeWarning(List<Statement> statement, List<ReturnStatement> rtn) {
        Span tempSpan = statement.get(0).getSpan();
        for (int i = 1; i < statement.size(); i++) {
            tempSpan = tempSpan.merge(statement.get(i).getSpan());
        }
        this.stmt = tempSpan;
        this.retnStmts = rtn;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("This code can never be reached.");
        this.addPrimaryAnnotation(stmt);
        for (ReturnStatement rtnStmt : retnStmts) {
            this.addSecondaryAnnotation(rtnStmt.getSpan());
        }
    }
}
