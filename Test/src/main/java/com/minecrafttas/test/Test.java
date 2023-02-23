package com.minecrafttas.test;

import net.fabricmc.api.ModInitializer;
//# 1.15.2
//# def
//$$import net.minecraft.client.Minecraft;
//# end

public class Test implements ModInitializer {

	@Override
	public void onInitialize() {
		System.out.println("pancake tampered with your game");
		// # 1.15.2
//$$		//HELP ME
		// # 1.14.4
//$$		Minecraft.getInstance();
		// # def
//$$		Minecraft.getMinecraft();
		// # end
	}
}
