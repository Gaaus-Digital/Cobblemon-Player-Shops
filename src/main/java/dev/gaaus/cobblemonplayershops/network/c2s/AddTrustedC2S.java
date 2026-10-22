package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Add a trusted player by name (owner only). */
public record AddTrustedC2S(int containerId, String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AddTrustedC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("add_trusted"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AddTrustedC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AddTrustedC2S::containerId,
		ByteBufCodecs.stringUtf8(16), AddTrustedC2S::name,
		AddTrustedC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
