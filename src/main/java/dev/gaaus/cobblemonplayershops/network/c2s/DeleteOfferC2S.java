package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Delete an offer. */
public record DeleteOfferC2S(int containerId, int offerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DeleteOfferC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("delete_offer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DeleteOfferC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, DeleteOfferC2S::containerId,
		ByteBufCodecs.VAR_INT, DeleteOfferC2S::offerId,
		DeleteOfferC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
