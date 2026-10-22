package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Change a trusted player's flags (owner only). */
public record SetTrustedPermsC2S(int containerId, UUID player, boolean canAccessInventory, boolean canWithdraw) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetTrustedPermsC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("set_trusted_perms"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetTrustedPermsC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SetTrustedPermsC2S::containerId,
		UUIDUtil.STREAM_CODEC, SetTrustedPermsC2S::player,
		ByteBufCodecs.BOOL, SetTrustedPermsC2S::canAccessInventory,
		ByteBufCodecs.BOOL, SetTrustedPermsC2S::canWithdraw,
		SetTrustedPermsC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
