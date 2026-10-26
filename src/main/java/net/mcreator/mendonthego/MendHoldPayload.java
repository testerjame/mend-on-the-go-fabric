package net.mcreator.mendonthego;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: whether the Mend key is currently held down. */
public record MendHoldPayload(boolean held) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MendHoldPayload> TYPE =
			new CustomPacketPayload.Type<>(MendOnTheGo.id("mend"));

	public static final StreamCodec<RegistryFriendlyByteBuf, MendHoldPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.BOOL, MendHoldPayload::held, MendHoldPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
