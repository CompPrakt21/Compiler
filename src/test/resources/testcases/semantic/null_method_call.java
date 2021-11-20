/* Methods cannot be called on `null` literals. */

class Main {
	public void bar() {}
	public static void main(String[] args) {
		null.bar();
	}
}
