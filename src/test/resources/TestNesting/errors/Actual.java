package TestNesting.errors;

public class Actual {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.16.1
		i = 2;
		// Code for 1.16.1
		// ## def
		int c = 3;
		i = 4;
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
