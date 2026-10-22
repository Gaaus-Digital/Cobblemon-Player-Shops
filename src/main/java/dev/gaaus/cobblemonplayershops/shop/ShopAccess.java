package dev.gaaus.cobblemonplayershops.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-trusted-player permissions. Identity is the UUID key of the owning map; the name is display only. */
public record ShopAccess(String lastKnownName, boolean canAccessInventory, boolean canWithdraw) {
	public static final Codec<ShopAccess> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.fieldOf("name").forGetter(ShopAccess::lastKnownName),
		Codec.BOOL.fieldOf("inventory").forGetter(ShopAccess::canAccessInventory),
		Codec.BOOL.fieldOf("withdraw").forGetter(ShopAccess::canWithdraw)
	).apply(i, ShopAccess::new));

	public boolean hasAnyAccess() {
		return canAccessInventory || canWithdraw;
	}
}
