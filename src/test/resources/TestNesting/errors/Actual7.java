package TestNesting.errors;

public class Actual7 {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.16.1
		i = 2;
		// Code for 1.16.1
		// ## 1.16.5
		// ### 1.18.2
		int c = 3;
		i = 4;
		// ### 1.18.2
		// ### end
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
