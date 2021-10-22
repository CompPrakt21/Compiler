package compiler.ast;

import java.util.Optional;

public final class ReturnStatement extends Statement {
    private Optional<Expression> expression;

    public ReturnStatement(Optional<Expression> expression) {
        this.expression = expression;
    }

}
