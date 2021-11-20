/* Local variables cannot be shadowed. */

class Main {
	public static void main(String[] args) {
		int i = 0;
		while (true) {
			int i = 1;
		}
	}
}
