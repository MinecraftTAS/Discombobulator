package TestPatternsInverted;

public class Actual {
	Minecraft mc = new Minecraft();
	
	public Actual() {
		Minecraft mc = Minecraft.getInstance(); //@GetMinecraft;
		mc.level=10;	//@GetLevel;
		Minecraft.getInstance().level=10; //@GetMinecraft,GetLevel;
		
		Minecraft mc1 = Minecraft.getMinecraft(); // @ GetMinecraft;
		mc1.world=10;	// @ GetLevel;
		Minecraft.getMinecraft().world=10; // @ GetMinecraft , GetLevel;
	}
	
	
	@SuppressWarnings("unused")
	private static class Minecraft {
		public int world=0;
		public int level=0;
		
		public static Minecraft getMinecraft() {
			return new Minecraft();
		}
		public static Minecraft getInstance() {
			return new Minecraft();
		}
	}
}
