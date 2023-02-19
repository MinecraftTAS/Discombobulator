package com.minecrafttas.test;

import net.fabricmc.api.ClientModInitializer;

public class ClientTest implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		System.out.println("Pancake tampered with your client");
		
	}

}
