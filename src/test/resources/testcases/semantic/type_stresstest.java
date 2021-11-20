/* OK */

class Main {
	public int x;
	public int y;
	public Main z;

	public Main foo(int x, int y) {
		z = null;
		if (x == y) {
			return z;
			boolean b = this == this;
			this.bar(); bar();
			return this.z = new Main();
		}
		return null;
	}

	public void bar() {

	}

	public static void main(String[] args) {
		int x = 1;
		int y = x;
		boolean z = x > y;
		while (z) {
			x = new Main().z.foo(x, new Main().y).foo(x, 1).z.y;
		}
		(new Main().z) = null;
		new Main().foo(1, 2).x = 1;
		(new Main()).bar();
		new Main().foo(3, 4);
		z = false;
		if (z = true) {
			return;
		}
		z = new Main() == new Main();
		z = new Main() != new Main();
	}
}
