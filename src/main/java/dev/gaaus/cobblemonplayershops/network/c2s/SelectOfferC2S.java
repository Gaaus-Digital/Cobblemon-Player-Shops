package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Load an existing offer into the template buffer. */
public record SelectOfferC2S(int containerId, int offerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SelectOfferC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("select_offer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SelectOfferC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SelectOfferC2S::containerId,
		ByteBufCodecs.VAR_INT, SelectOfferC2S::offerId,
		SelectOfferC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
