package compiler.errors;

import compiler.ast.Method;
import compiler.ast.Parameter;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class IllegalMethodParameterType extends CompilerError {
    public enum Reason {
        UNRESOLVEABLE,
        VOID,
    }

    private Parameter param;
    private Reason reason;

    public IllegalMethodParameterType(Parameter param, Reason reason) {
        this.param = param;
        this.reason = reason;
    }

    @Override
    public void generate(Source source) {
        switch (this.reason) {
            case UNRESOLVEABLE -> {
                var tyStr = source.getSpanString(this.param.getType().getSpan());
                this.setMessage(String.format("Can not resolve type '%s'", tyStr));
            }
            case VOID -> {
                this.setMessage("Parameter can not have type void");
            }
        }

        this.addPrimaryAnnotation(this.param.getSpan());
    }

}
