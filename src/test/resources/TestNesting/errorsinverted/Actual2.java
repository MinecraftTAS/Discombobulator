package TestNesting.errorsinverted;

public class Actual2 {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.8.9
		i = 2;
		// Code for 1.8.9
		// ## 1.11.2
		int c = 3;
		i = 4;
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
