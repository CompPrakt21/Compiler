package compiler.codegen;

public enum Predicate {
    GREATER_THAN("g"), GREATER_EQUAL("ge"), LESS_THAN("l"), LESS_EQUAL("le"), EQUAL("e"), NOT_EQUAL("ne");

    private final String suffix;

    Predicate(String suffix) {
                           this.suffix = suffix;
                                                }

    public Predicate invert() {
        return switch (this) {
            case GREATER_THAN -> LESS_EQUAL;
            case GREATER_EQUAL -> LESS_THAN;
            case LESS_THAN -> GREATER_EQUAL;
            case LESS_EQUAL -> GREATER_THAN;
            case EQUAL -> NOT_EQUAL;
            case NOT_EQUAL -> EQUAL;
        };
    }

    /**
     * @return How the predicate needs to change if the arguments of cmp are swapped.
     */
    public Predicate withSwappedArguments() {
        return switch (this) {
            case GREATER_THAN -> LESS_THAN;
            case GREATER_EQUAL -> LESS_EQUAL;
            case LESS_EQUAL -> GREATER_EQUAL;
            case LESS_THAN -> GREATER_THAN;
            case EQUAL -> EQUAL;
            case NOT_EQUAL -> NOT_EQUAL;
        };
    }

    public String getSuffix() {
                            return suffix;
                                          }
}