package dev.gaaus.cobblemonplayershops.menu;

import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Entry points for opening shop menus: the merchant view (everyone) and the stock view (inventory permission). */
public final class ShopMenus {
	private ShopMenus() {
	}

	/** The merchant view: offers, buying, inline offer editing, withdraw and settings. Never has slots. */
	public static void openMerchantMenu(ServerPlayer player, ShopBlockEntity shop) {
		open(player, new ExtendedScreenHandlerFactory<ShopMenuData>() {
			@Override
			public ShopMenuData getScreenOpeningData(ServerPlayer p) {
				return ShopMenuData.create(shop, p, ItemStack.EMPTY);
			}

			@Override
			public Component getDisplayName() {
				return shop.getDisplayName();
			}

			@Nullable
			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				return new ShopBuyMenu(containerId, inventory, shop);
			}
		});
	}

	/**
	 * The stock view. Refused (returns false) for admin shops and for viewers without inventory permission;
	 * checked live here, not taken from any client flag.
	 */
	public static boolean openInventoryMenu(ServerPlayer player, ShopBlockEntity shop) {
		if (shop.isAdminShop() || !shop.canAccessInventory(player)) {
			return false;
		}
		open(player, new ExtendedScreenHandlerFactory<ShopMenuData>() {
			@Override
			public ShopMenuData getScreenOpeningData(ServerPlayer p) {
				return ShopMenuData.create(shop, p, ItemStack.EMPTY);
			}

			@Override
			public Component getDisplayName() {
				return shop.getDisplayName();
			}

			@Nullable
			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
				return new ShopOwnerMenu(containerId, inventory, shop);
			}
		});
		return true;
	}

	/**
	 * Closes a currently open shop menu server-side only (no close packet), so the client swaps screens
	 * without re-centring the mouse. {@code removed()} still hands any carried stack back to the player.
	 */
	private static void open(ServerPlayer player, ExtendedScreenHandlerFactory<ShopMenuData> factory) {
		if (player.containerMenu instanceof ShopMenu) {
			player.doCloseContainer();
		}
		player.openMenu(factory);
	}
}
