package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Toggle hopper input (player shop only). */
public record ToggleHopperC2S(int containerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ToggleHopperC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("toggle_hopper"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleHopperC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ToggleHopperC2S::containerId,
		ToggleHopperC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
