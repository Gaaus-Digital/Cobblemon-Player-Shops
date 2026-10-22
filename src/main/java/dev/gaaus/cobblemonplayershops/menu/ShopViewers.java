package dev.gaaus.cobblemonplayershops.menu;

import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import java.util.ArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Finds everyone with a menu open on a shop and refreshes (or closes) it. */
public final class ShopViewers {
	private ShopViewers() {
	}

	/**
	 * Re-evaluates every open menu on {@code shop}: viewers whose role changed get fresh flags, viewers who
	 * lost all management permission are closed.
	 */
	public static void refreshAll(ShopBlockEntity shop) {
		if (shop.getLevel() == null || shop.getLevel().getServer() == null) {
			return;
		}
		for (ServerPlayer player : new ArrayList<>(shop.getLevel().getServer().getPlayerList().getPlayers())) {
			AbstractContainerMenu menu = player.containerMenu;
			if (menu instanceof ShopOwnerMenu owner && owner.shop() == shop) {
				owner.sendRefresh(player);
			} else if (menu instanceof ShopBuyMenu buy && buy.shop() == shop) {
				buy.sendRefresh(player);
			}
		}
	}
}
