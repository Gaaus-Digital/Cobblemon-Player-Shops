package dev.gaaus.cobblemonplayershops.registry;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.menu.ShopBuyMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData;
import dev.gaaus.cobblemonplayershops.menu.ShopOwnerMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModMenus {
	public static final ExtendedScreenHandlerType<ShopOwnerMenu, ShopMenuData> OWNER = Registry.register(BuiltInRegistries.MENU,
		CobblemonPlayerShops.id("shop_owner"), new ExtendedScreenHandlerType<>(ShopOwnerMenu::new, ShopMenuData.STREAM_CODEC));

	public static final ExtendedScreenHandlerType<ShopBuyMenu, ShopMenuData> BUY = Registry.register(BuiltInRegistries.MENU,
		CobblemonPlayerShops.id("shop_buy"), new ExtendedScreenHandlerType<>(ShopBuyMenu::new, ShopMenuData.STREAM_CODEC));

	private ModMenus() {
	}

	public static void init() {
		// class load registers the menu types
	}
}
