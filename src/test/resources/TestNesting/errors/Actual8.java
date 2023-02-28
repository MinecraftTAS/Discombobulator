package TestNesting.errors;

public class Actual8 {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.16.1
		i = 2;
		// Code for 1.16.1
		// ## def
		// ## 1.16.5
		// ### 1.18.2
		int c = 3;
		i = 4;
		// ### end
		// ## def
		// DUPLICATE DEF
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
