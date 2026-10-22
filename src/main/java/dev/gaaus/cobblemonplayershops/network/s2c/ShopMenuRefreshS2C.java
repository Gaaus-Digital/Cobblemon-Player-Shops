package dev.gaaus.cobblemonplayershops.network.s2c;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fresh menu data for an open shop menu (stock counts, offers, vault, flags). */
public record ShopMenuRefreshS2C(int containerId, ShopMenuData data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ShopMenuRefreshS2C> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("menu_refresh"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ShopMenuRefreshS2C> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ShopMenuRefreshS2C::containerId,
		ShopMenuData.STREAM_CODEC, ShopMenuRefreshS2C::data,
		ShopMenuRefreshS2C::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
