package com.minecrafttas.discombobulator.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ProcessorTestPatterns {

	Map<String, Map<String, String>> patterns = Map.of(
			"GetLevel", Map.of(
				"1.14.4", "Level level = mc.level",
				"1.12.2", "World level = mc.world"
			),
			"GetMinecraft", Map.of(
				"1.14.4", "Minecraft.getInstance()",
				"1.12.2", "Minecraft.getMinecraft()"
			)
		);

	@Test
	void testPattern() {
	}

}
