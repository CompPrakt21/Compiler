/* Method invocation is missing parentheses */

class Foo {
	public int i;

	public void bar() {
	}
}

class Main {
	public static void main(String[] args) {
		Foo foo = new Foo();

		foo.bar;
	}
}
