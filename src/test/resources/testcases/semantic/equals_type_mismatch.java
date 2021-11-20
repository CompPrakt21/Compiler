/* `==` can only be used for the same type. */

class Bar {

}

class Main {
	public static void main(String[] args) {
		if (new Main() == new Bar()) {
			return;
		}
	}
}
