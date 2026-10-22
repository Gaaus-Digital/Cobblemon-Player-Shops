package dev.gaaus.cobblemonplayershops.client;

import dev.gaaus.cobblemonplayershops.client.render.ShopBlockEntityRenderer;
import dev.gaaus.cobblemonplayershops.client.screen.ShopInventoryScreen;
import dev.gaaus.cobblemonplayershops.client.screen.ShopMerchantScreen;
import dev.gaaus.cobblemonplayershops.menu.ShopBuyMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopOwnerMenu;
import dev.gaaus.cobblemonplayershops.network.s2c.ShopMenuRefreshS2C;
import dev.gaaus.cobblemonplayershops.registry.ModBlockEntities;
import dev.gaaus.cobblemonplayershops.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class CobblemonPlayerShopsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(ModBlockEntities.SHOP, ShopBlockEntityRenderer::new);
		MenuScreens.register(ModMenus.OWNER, ShopInventoryScreen::new);
		MenuScreens.register(ModMenus.BUY, ShopMerchantScreen::new);

		// Fabric runs play receivers on the client main thread.
		ClientPlayNetworking.registerGlobalReceiver(ShopMenuRefreshS2C.TYPE, (payload, context) -> {
			Player player = context.player();
			AbstractContainerMenu menu = player.containerMenu;
			if (menu.containerId != payload.containerId()) {
				return;
			}
			Minecraft minecraft = Minecraft.getInstance();
			if (menu instanceof ShopOwnerMenu owner) {
				owner.applyData(payload.data());
				if (minecraft.screen instanceof ShopInventoryScreen screen) {
					screen.onDataRefreshed();
				}
			} else if (menu instanceof ShopBuyMenu buy) {
				buy.applyData(payload.data());
				if (minecraft.screen instanceof ShopMerchantScreen screen) {
					screen.onDataRefreshed();
				}
			}
		});
	}
}
