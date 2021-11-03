/* This should fail because the statement after an else can't be a local variable statement. */

class Main {
	public static void main(String[] args) {
		if (true) {} else int x = 123;
	}
}
