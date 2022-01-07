package compiler.codegen;

public enum Predicate {
    GREATER_THAN("g"), GREATER_EQUAL("ge"), LESS_THAN("l"), LESS_EQUAL("le"), EQUAL("e"), NOT_EQUAL("ne");

    private String suffix;

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

    public String getSuffix() {
                            return suffix;
                                          }
}