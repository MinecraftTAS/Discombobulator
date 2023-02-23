package TestNesting.errorsinverted;

public class Actual {
	@SuppressWarnings("unused")
	public void nestingTest() {
		int i;
		// # 1.12
		i = 2;
		// Code for 1.12
		// ## def
		int c = 3;
		i = 4;
		// ## end
		//# def
		i = 1;
		//# end
		
	}
}
