package dev.gaaus.cobblemonplayershops.menu;

import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import org.jetbrains.annotations.Nullable;

/** Implemented by both shop menus so the shop can find and close its viewers. */
public interface ShopMenu {
	/** The server-side shop this menu is bound to, or null on the client. */
	@Nullable
	ShopBlockEntity shop();
}
