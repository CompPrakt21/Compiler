/* OK blockwise reinstanciations are allowed */

class Main {
	int i = -1;
	public static void main(String[] args) {
		int i = 0;
		{
			int i = 1;
			{
				int i = 2;
			}
		}
	}
}
