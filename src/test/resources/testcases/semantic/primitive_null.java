/* `null` is not usable for primitive types. */

class Main {
	public static void main(String[] args) {
		if (1 < null) {
			return;
		}
	}
}
