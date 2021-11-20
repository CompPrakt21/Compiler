/* Variables cannot be used outside of their scope of declaration. */

class Main {
	public static void main(String[] args) {
		while (true) {
			int i = 0;
		}
		boolean z = i == 0;
	}
}
