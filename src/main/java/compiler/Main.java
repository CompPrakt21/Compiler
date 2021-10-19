package compiler;

public class Main {

    public static void main(String[] args) {

        // Demonstrate Java 17 with Preview features works
        Object a = 114;
        String formatted = switch (a) {
            case Integer i && i > 10 -> String.format("a large Integer %d", i);
            case Integer i -> String.format("a small Integer %d", i);
            default -> "something else";
        };
        System.out.println("Hello World: " + formatted);
    }

    public static int getRandomInt() {
        return 12; // Randomly selected
    }

}
