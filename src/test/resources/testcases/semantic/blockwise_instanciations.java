/* OK blockwise reinstanciations are allowed */

class Main {
	public int i;
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
