package TestNesting.errorsinverted;

public class Actual3 {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.12
		i = 2;
		// Code for 1.12
		// ## 1.11.2
		int c = 3;
		i = 4;
		// ## end
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
