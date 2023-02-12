package Test4;

public class Actual1 {
	Minecraft mc = new Minecraft();
	
	public Actual1() {
		Minecraft mc = Minecraft.getInstance(); //@GetMinecraft111111111111111111;
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
