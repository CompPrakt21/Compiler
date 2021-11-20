/* OK */

class main {
	public int main;
	public static void main(String[] args) {
		main main = new main();
		int x = main.main + main.main;
		bar bar = new bar();
		int y = bar.bar.x + bar.bar().x;
	}
}

class bar {
	public int x;
	public bar bar() {
		bar bar = bar;
		bar = new bar();
		bar.x = bar.x + bar().x;
		return this.bar;
	}
	public bar bar;
}
