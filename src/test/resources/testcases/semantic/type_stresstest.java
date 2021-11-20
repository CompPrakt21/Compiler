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
		Main[] foo1 = new Main[x = -1];
		Main[][] foo2 = new Main[new int[1][0]][];
		foo1 = foo2[0];
		foo2[0] = foo1;
		foo1[0] = null;
		foo2[0][0] = null;
		foo1 = null;
		foo2[0][0].foo(0, 0);
		int[] foo3 = null;
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
