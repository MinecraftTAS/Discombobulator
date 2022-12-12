package com.minecrafttas.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.TitleScreen;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
	public void onDraw(TitleScreen g, Font font, String string, int i, int j, int k) {
		g.drawString(font, "haha pancake was here", i, j, k);
	}

}
