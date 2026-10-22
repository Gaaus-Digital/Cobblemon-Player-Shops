package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Remove a trusted player (owner only). */
public record RemoveTrustedC2S(int containerId, UUID player) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RemoveTrustedC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("remove_trusted"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RemoveTrustedC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, RemoveTrustedC2S::containerId,
		UUIDUtil.STREAM_CODEC, RemoveTrustedC2S::player,
		RemoveTrustedC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
