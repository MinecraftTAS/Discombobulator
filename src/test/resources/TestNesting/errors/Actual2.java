package TestNesting.errors;

public class Actual2 {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.20.0
		i = 2;
		// Code for 1.16.1
		// ## infinity
		int c = 3;
		i = 4;
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
