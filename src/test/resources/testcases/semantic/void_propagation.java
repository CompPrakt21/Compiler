/* `void` should not propagate. */

class Main {
	public void foo() {}
	public static void main(String[] args) {
		return new Main().foo();
	}
}
