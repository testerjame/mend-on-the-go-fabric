package net.mcreator.mendonthego;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class MendOnTheGo implements ModInitializer {
	public static final String MOD_ID = "mend_on_the_go";

	/** Persistent per-player toggle controlled by /repair (kept across restarts and death). */
	public static final AttachmentType<Boolean> AUTO_REPAIR = AttachmentRegistry.create(
			id("auto_repair"),
			builder -> builder.initializer(() -> false).persistent(Codec.BOOL).copyOnDeath());

	/** Players currently holding the Mend key (server thread only, never persisted). */
	private static final Set<UUID> HOLDING = new HashSet<>();

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Key held / released, sent by the client.
		PayloadTypeRegistry.serverboundPlay().register(MendHoldPayload.TYPE, MendHoldPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(MendHoldPayload.TYPE, (payload, context) -> {
			UUID id = context.player().getUUID();
			if (payload.held()) {
				HOLDING.add(id);
			} else {
				HOLDING.remove(id);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> HOLDING.remove(listener.getPlayer().getUUID()));

		// /repair toggles automatic repair of the held item.
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("repair").executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayer();
					if (player == null) {
						return 0;
					}
					boolean enabled = !player.getAttachedOrElse(AUTO_REPAIR, false);
					player.setAttached(AUTO_REPAIR, enabled);
					player.sendSystemMessage(Component.literal(enabled
							? "Items now automatically repair when held!"
							: "Items won't automatically repair when held!"));
					return 1;
				})));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (HOLDING.contains(player.getUUID()) || player.getAttachedOrElse(AUTO_REPAIR, false)) {
					tryMend(player);
				}
			}
		});
	}

	/** Every tick: 1 XP point -> 2 durability on the held Mending item. */
	private static void tryMend(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty() || stack.getDamageValue() <= 0) {
			return;
		}
		var mending = player.level().registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.MENDING);
		if (EnchantmentHelper.getItemEnchantmentLevel(mending, stack) <= 0) {
			return;
		}
		boolean hasXp = player.experienceLevel > 0
				|| player.experienceProgress * player.getXpNeededForNextLevel() >= 1.0F;
		if (!hasXp) {
			return;
		}
		stack.setDamageValue(Math.max(0, stack.getDamageValue() - 2));
		player.giveExperiencePoints(-1);
	}
}
