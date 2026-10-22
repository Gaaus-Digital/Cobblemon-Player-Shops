package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Withdraw the vault. */
public record WithdrawC2S(int containerId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<WithdrawC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("withdraw"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WithdrawC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, WithdrawC2S::containerId,
		WithdrawC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
