package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Switch from the stock view back to the merchant view. */
public record OpenMerchantC2S(int containerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenMerchantC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("open_merchant"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenMerchantC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenMerchantC2S::containerId,
		OpenMerchantC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
