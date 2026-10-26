package net.mcreator.mendonthego.client;

import org.lwjgl.sdl.SDLScancode;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.mcreator.mendonthego.MendHoldPayload;

public class MendOnTheGoClient implements ClientModInitializer {
	public static final KeyMapping MEND = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.mend_on_the_go.mend", InputConstants.Type.KEYBOARD, SDLScancode.SDL_SCANCODE_LSHIFT, KeyMapping.Category.GAMEPLAY));

	private boolean lastSent = false;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.getConnection() == null) {
				lastSent = false;
				return;
			}
			// Like the original: only counts while no screen is open.
			boolean down = client.screen == null && MEND.isDown();
			if (down != lastSent) {
				lastSent = down;
				ClientPlayNetworking.send(new MendHoldPayload(down));
			}
		});
	}
}
