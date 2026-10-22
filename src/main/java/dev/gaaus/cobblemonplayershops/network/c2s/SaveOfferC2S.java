package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Save the buffered template as an offer. replaceOfferId = -1 for a new one. */
public record SaveOfferC2S(int containerId, int replaceOfferId, long price, int units) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SaveOfferC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("save_offer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SaveOfferC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SaveOfferC2S::containerId,
		ByteBufCodecs.VAR_INT, SaveOfferC2S::replaceOfferId,
		ByteBufCodecs.VAR_LONG, SaveOfferC2S::price,
		ByteBufCodecs.VAR_INT, SaveOfferC2S::units,
		SaveOfferC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
