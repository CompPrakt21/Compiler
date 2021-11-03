/* This should fail because the statement after an while can't be a local variable statement. */

class Main {
	public static void main(String[] args) {
		while (true) int x = 123;
	}
}
