package dev.gaaus.cobblemonplayershops.shop;

import com.mojang.authlib.GameProfile;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.log.TransactionLog;
import dev.gaaus.cobblemonplayershops.menu.ShopBuyMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopViewers;
import dev.gaaus.cobblemonplayershops.registry.ModTags;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Offer editing and access (trusted player) management. Every method re-checks the permission it needs
 * against the live shop state; client flags are never trusted.
 */
public final class ShopManagement {
	private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

	private ShopManagement() {
	}

	private static void tell(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable("message.cobblemon_player_shops." + key, args), false);
	}

	// ---------------------------------------------------------------- offers

	/**
	 * Copies the SERVER's own inventory stack in {@code inventorySlot} into the buffer. The client only names a
	 * slot (-1 clears) and never supplies an item.
	 */
	public static void setTemplate(ShopBuyMenu menu, ServerPlayer player, int inventorySlot) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canEditOffers(player)) {
			return;
		}
		if (inventorySlot < -1 || inventorySlot >= Inventory.INVENTORY_SIZE) {
			return;
		}
		ItemStack stack = inventorySlot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(inventorySlot);
		if (!stack.isEmpty() && stack.is(ModTags.UNSELLABLE)) {
			tell(player, "item_unsellable");
			return;
		}
		menu.setTemplateBuffer(stack);
		menu.sendRefresh(player);
	}

	public static void selectOffer(ShopBuyMenu menu, ServerPlayer player, int offerId) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canEditOffers(player)) {
			return;
		}
		ShopOffer offer = shop.findOffer(offerId);
		if (offer == null) {
			menu.sendRefresh(player);
			return;
		}
		menu.setTemplateBuffer(offer.template());
		menu.sendRefresh(player);
	}

	public static void saveOffer(ShopBuyMenu menu, ServerPlayer player, int replaceId, long price, int units) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canEditOffers(player)) {
			return;
		}
		ShopConfig config = ShopConfig.get();
		ItemStack template = menu.templateBuffer();
		if (template.isEmpty()) {
			tell(player, "no_template");
			return;
		}
		if (template.is(ModTags.UNSELLABLE)) {
			tell(player, "item_unsellable");
			return;
		}
		if (price < 1 || price > config.maxPrice) {
			tell(player, "invalid_price", config.maxPrice);
			return;
		}
		if (units < 1 || units > template.getMaxStackSize()) {
			tell(player, "invalid_units", template.getMaxStackSize());
			return;
		}
		ShopOffer saved = shop.saveOffer(replaceId, template.copyWithCount(units), price);
		if (saved == null) {
			tell(player, "offers_full", config.maxOffersPerShop);
			return;
		}
		TransactionLog.log("OFFER_SAVE", shop, player, "offer=" + saved.id() + " item=" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(template.getItem())
			+ " units=" + units + " price=" + price);
		ShopViewers.refreshAll(shop);
	}

	public static void deleteOffer(ShopBuyMenu menu, ServerPlayer player, int offerId) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canEditOffers(player)) {
			return;
		}
		if (shop.removeOffer(offerId)) {
			TransactionLog.log("OFFER_DELETE", shop, player, "offer=" + offerId);
		}
		ShopViewers.refreshAll(shop);
	}

	public static void toggleHopper(ShopMenu menu, ServerPlayer player) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || shop.isAdminShop() || !shop.canEditOffers(player)) {
			return;
		}
		shop.setHopperInput(!shop.isHopperInput());
		TransactionLog.log("HOPPER_INPUT", shop, player, "enabled=" + shop.isHopperInput());
		ShopViewers.refreshAll(shop);
	}

	// ---------------------------------------------------------------- access (owner only; admins/ops do not bypass)

	public static void addTrusted(ShopMenu menu, ServerPlayer player, String name) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canManageAccess(player)) {
			return;
		}
		if (name == null || !NAME.matcher(name).matches()) {
			tell(player, "invalid_player_name");
			return;
		}
		MinecraftServer server = player.server;
		GameProfile profile = null;
		ServerPlayer online = server.getPlayerList().getPlayerByName(name);
		if (online != null) {
			profile = online.getGameProfile();
		} else if (server.getProfileCache() != null) {
			Optional<GameProfile> cached = server.getProfileCache().get(name);
			profile = cached.orElse(null);
		}
		if (profile == null) {
			tell(player, "unknown_player", name);
			return;
		}
		UUID id = profile.getId();
		if (id.equals(player.getUUID())) {
			tell(player, "cannot_trust_self");
			return;
		}
		if (shop.getTrusted().containsKey(id)) {
			tell(player, "already_trusted", profile.getName());
			return;
		}
		if (shop.getTrusted().size() >= ShopConfig.get().maxTrustedPerShop) {
			tell(player, "trusted_full", ShopConfig.get().maxTrustedPerShop);
			return;
		}
		// Least privilege for money: inventory access on, withdraw off.
		shop.putTrusted(id, new ShopAccess(profile.getName(), true, false));
		TransactionLog.log("TRUST_ADD", shop, player, "target=" + id + "(" + profile.getName() + ") inventory=true withdraw=false");
		ShopViewers.refreshAll(shop);
	}

	public static void removeTrusted(ShopMenu menu, ServerPlayer player, UUID target) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canManageAccess(player)) {
			return;
		}
		if (shop.removeTrusted(target)) {
			TransactionLog.log("TRUST_REMOVE", shop, player, "target=" + target);
		}
		ShopViewers.refreshAll(shop);
	}

	public static void setTrustedPerms(ShopMenu menu, ServerPlayer player, UUID target, boolean inventory, boolean withdraw) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.canManageAccess(player)) {
			return;
		}
		ShopAccess current = shop.getTrusted().get(target);
		if (current == null) {
			ShopViewers.refreshAll(shop);
			return;
		}
		shop.putTrusted(target, new ShopAccess(current.lastKnownName(), inventory, withdraw));
		TransactionLog.log("TRUST_PERMS", shop, player, "target=" + target + " inventory=" + inventory + " withdraw=" + withdraw);
		// Re-evaluates every open menu: changed roles get new flags, viewers with no permission left are closed.
		ShopViewers.refreshAll(shop);
	}
}
