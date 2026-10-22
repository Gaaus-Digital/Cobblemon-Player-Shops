package dev.gaaus.cobblemonplayershops.network.c2s;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Copy the stack in one of the sender's own inventory slots (0..35, -1 clears) into the template buffer. Carries no item. */
public record SetTemplateC2S(int containerId, int inventorySlot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SetTemplateC2S> TYPE = new CustomPacketPayload.Type<>(CobblemonPlayerShops.id("set_template"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SetTemplateC2S> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, SetTemplateC2S::containerId,
		ByteBufCodecs.VAR_INT, SetTemplateC2S::inventorySlot,
		SetTemplateC2S::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
