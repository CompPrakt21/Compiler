package compiler.ast;

import java.util.Optional;

public final class ReturnStatement extends Statement {
    private Optional<Expression> expression;
}
