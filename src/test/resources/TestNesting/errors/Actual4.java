package TestNesting.errors;

public class Actual4 {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.16.1
		i = 2;
		// Code for 1.16.1
		// ## 1.16.5
		int c = 3;
		i = 4;
		//# def
		i = 1;
		//# end
		
	}
}
