package dev.gaaus.cobblemonplayershops.network;

import dev.gaaus.cobblemonplayershops.menu.ShopBuyMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopMenus;
import dev.gaaus.cobblemonplayershops.menu.ShopOwnerMenu;
import dev.gaaus.cobblemonplayershops.network.c2s.AddTrustedC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.BuyOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.DeleteOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.OpenInventoryC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.OpenMerchantC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.RemoveTrustedC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SaveOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SelectOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SetTemplateC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SetTrustedPermsC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.ToggleHopperC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.WithdrawC2S;
import dev.gaaus.cobblemonplayershops.network.s2c.ShopMenuRefreshS2C;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import dev.gaaus.cobblemonplayershops.shop.ShopManagement;
import dev.gaaus.cobblemonplayershops.shop.ShopProtection;
import dev.gaaus.cobblemonplayershops.shop.ShopTransactions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Payload registration and the server-side receivers. Fabric API runs play receivers on the server thread.
 * Every receiver goes through {@link #guard} or {@link #guardAny}: the shop comes from the player's open menu,
 * never from the packet.
 */
public final class ModPayloads {
	private ModPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(ShopMenuRefreshS2C.TYPE, ShopMenuRefreshS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(SetTemplateC2S.TYPE, SetTemplateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SelectOfferC2S.TYPE, SelectOfferC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SaveOfferC2S.TYPE, SaveOfferC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DeleteOfferC2S.TYPE, DeleteOfferC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(ToggleHopperC2S.TYPE, ToggleHopperC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(WithdrawC2S.TYPE, WithdrawC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(BuyOfferC2S.TYPE, BuyOfferC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AddTrustedC2S.TYPE, AddTrustedC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoveTrustedC2S.TYPE, RemoveTrustedC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SetTrustedPermsC2S.TYPE, SetTrustedPermsC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenInventoryC2S.TYPE, OpenInventoryC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenMerchantC2S.TYPE, OpenMerchantC2S.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SetTemplateC2S.TYPE, (p, ctx) -> {
			ShopBuyMenu menu = guard(ctx.player(), p.containerId(), ShopBuyMenu.class);
			if (menu != null) {
				ShopManagement.setTemplate(menu, ctx.player(), p.inventorySlot());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(SelectOfferC2S.TYPE, (p, ctx) -> {
			ShopBuyMenu menu = guard(ctx.player(), p.containerId(), ShopBuyMenu.class);
			if (menu != null) {
				ShopManagement.selectOffer(menu, ctx.player(), p.offerId());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(SaveOfferC2S.TYPE, (p, ctx) -> {
			ShopBuyMenu menu = guard(ctx.player(), p.containerId(), ShopBuyMenu.class);
			if (menu != null) {
				ShopManagement.saveOffer(menu, ctx.player(), p.replaceOfferId(), p.price(), p.units());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(DeleteOfferC2S.TYPE, (p, ctx) -> {
			ShopBuyMenu menu = guard(ctx.player(), p.containerId(), ShopBuyMenu.class);
			if (menu != null) {
				ShopManagement.deleteOffer(menu, ctx.player(), p.offerId());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(ToggleHopperC2S.TYPE, (p, ctx) -> {
			ShopMenu menu = guardAny(ctx.player(), p.containerId());
			if (menu != null) {
				ShopManagement.toggleHopper(menu, ctx.player());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(WithdrawC2S.TYPE, (p, ctx) -> {
			ShopMenu menu = guardAny(ctx.player(), p.containerId());
			if (menu != null) {
				ShopBlockEntity shop = menu.shop();
				if (shop != null) {
					ShopTransactions.withdraw(ctx.player(), shop);
				}
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(BuyOfferC2S.TYPE, (p, ctx) -> {
			ShopBuyMenu menu = guard(ctx.player(), p.containerId(), ShopBuyMenu.class);
			if (menu != null) {
				ShopTransactions.buy(ctx.player(), menu, p.offerId(), p.expectedPrice(), p.quantity());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(AddTrustedC2S.TYPE, (p, ctx) -> {
			ShopMenu menu = guardAny(ctx.player(), p.containerId());
			if (menu != null) {
				ShopManagement.addTrusted(menu, ctx.player(), p.name());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(RemoveTrustedC2S.TYPE, (p, ctx) -> {
			ShopMenu menu = guardAny(ctx.player(), p.containerId());
			if (menu != null) {
				ShopManagement.removeTrusted(menu, ctx.player(), p.player());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(SetTrustedPermsC2S.TYPE, (p, ctx) -> {
			ShopMenu menu = guardAny(ctx.player(), p.containerId());
			if (menu != null) {
				ShopManagement.setTrustedPerms(menu, ctx.player(), p.player(), p.canAccessInventory(), p.canWithdraw());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(OpenInventoryC2S.TYPE, (p, ctx) -> {
			ShopBuyMenu menu = guard(ctx.player(), p.containerId(), ShopBuyMenu.class);
			if (menu != null && menu.shop() != null) {
				ShopMenus.openInventoryMenu(ctx.player(), menu.shop());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(OpenMerchantC2S.TYPE, (p, ctx) -> {
			ShopOwnerMenu menu = guard(ctx.player(), p.containerId(), ShopOwnerMenu.class);
			if (menu != null && menu.shop() != null) {
				ShopMenus.openMerchantMenu(ctx.player(), menu.shop());
			}
		});
	}

	/**
	 * Common guard for every C2S handler: real player, expected menu type, matching container id, shop
	 * still valid right now (vanilla only checks once per tick), and the rate limit.
	 */
	@Nullable
	private static <M extends AbstractContainerMenu & ShopMenu> M guard(ServerPlayer player, int containerId, Class<M> type) {
		AbstractContainerMenu menu = guardMenu(player, containerId);
		return menu != null && type.isInstance(menu) ? type.cast(menu) : null;
	}

	/** Same guard, for packets that either shop menu may send (permissions are still checked live per action). */
	@Nullable
	private static ShopMenu guardAny(ServerPlayer player, int containerId) {
		AbstractContainerMenu menu = guardMenu(player, containerId);
		return menu instanceof ShopMenu shopMenu ? shopMenu : null;
	}

	@Nullable
	private static AbstractContainerMenu guardMenu(ServerPlayer player, int containerId) {
		if (!ShopProtection.isRealPlayer(player)) {
			return null;
		}
		AbstractContainerMenu menu = player.containerMenu;
		if (!(menu instanceof ShopMenu typed) || menu.containerId != containerId) {
			return null;
		}
		ShopBlockEntity shop = typed.shop();
		if (shop == null || !shop.stillValid(player) || !menu.stillValid(player)) {
			return null;
		}
		if (!RateLimiter.tryAcquire(player)) {
			return null;
		}
		return menu;
	}
}
