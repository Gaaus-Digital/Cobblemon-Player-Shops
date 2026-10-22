package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Switch from the merchant view to the stock view. */
public record OpenInventoryC2S(int containerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenInventoryC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("open_inventory"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenInventoryC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, OpenInventoryC2S::containerId,
		OpenInventoryC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
