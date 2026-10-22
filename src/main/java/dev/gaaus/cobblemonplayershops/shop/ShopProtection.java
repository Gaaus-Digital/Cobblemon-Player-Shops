package dev.gaaus.cobblemonplayershops.shop;

import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Validity / range / identity checks shared by blocks, menus and packet handlers. */
public final class ShopProtection {
	private ShopProtection() {
	}

	/** A real, living, non-fake server player. */
	public static boolean isRealPlayer(Player player) {
		return player instanceof ServerPlayer sp && !(player instanceof FakePlayer) && sp.isAlive() && !sp.isSpectator();
	}

	/** Live check that {@code shop} is still the block entity at its position and {@code player} is in range. */
	public static boolean canStillUse(Player player, ShopBlockEntity shop) {
		Level level = shop.getLevel();
		if (level == null || shop.isRemoved() || level != player.level()) {
			return false;
		}
		BlockPos pos = shop.getBlockPos();
		if (level.getBlockEntity(pos) != shop || !(level.getBlockState(pos).getBlock() instanceof ShopBlock)) {
			return false;
		}
		double range = ShopConfig.get().interactionRange;
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= range * range;
	}
}
