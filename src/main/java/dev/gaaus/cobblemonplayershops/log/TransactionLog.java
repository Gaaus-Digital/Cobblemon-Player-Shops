package dev.gaaus.cobblemonplayershops.log;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** One INFO line per buy/withdraw/payout/offer change. Disabled by config.logTransactions. */
public final class TransactionLog {
	private TransactionLog() {
	}

	public static void log(String event, ShopBlockEntity shop, @Nullable Player actor, String detail) {
		if (!ShopConfig.get().logTransactions) {
			return;
		}
		CobblemonPlayerShops.LOGGER.info("[shop-tx] {} dim={} pos={} owner={} actor={} {} vault={}",
			event, dimension(shop), pos(shop.getBlockPos()), shop.getOwner(), actor(actor), detail, shop.getVault());
	}

	/** For events where the actor may be known only by UUID or not at all (e.g. removal by /setblock). */
	public static void logRaw(String event, ShopBlockEntity shop, @Nullable UUID actorId, String detail) {
		if (!ShopConfig.get().logTransactions) {
			return;
		}
		CobblemonPlayerShops.LOGGER.info("[shop-tx] {} dim={} pos={} owner={} actor={} {} vault={}",
			event, dimension(shop), pos(shop.getBlockPos()), shop.getOwner(), actorId, detail, shop.getVault());
	}

	private static String dimension(ShopBlockEntity shop) {
		return shop.getLevel() == null ? "?" : shop.getLevel().dimension().location().toString();
	}

	private static String pos(BlockPos p) {
		return p.getX() + "," + p.getY() + "," + p.getZ();
	}

	private static String actor(@Nullable Player actor) {
		return actor == null ? "none" : actor.getUUID() + "(" + actor.getGameProfile().getName() + ")";
	}
}
