package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Buy {quantity} purchases of an offer at the price the buyer saw. */
public record BuyOfferC2S(int containerId, int offerId, long expectedPrice, int quantity) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BuyOfferC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("buy_offer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuyOfferC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, BuyOfferC2S::containerId,
		ByteBufCodecs.VAR_INT, BuyOfferC2S::offerId,
		ByteBufCodecs.VAR_LONG, BuyOfferC2S::expectedPrice,
		ByteBufCodecs.VAR_INT, BuyOfferC2S::quantity,
		BuyOfferC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
