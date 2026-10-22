package dev.gaaus.cobblemonplayershops.shop;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.economy.CobbleDollarsEconomy;
import dev.gaaus.cobblemonplayershops.economy.Economy;
import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.log.TransactionLog;
import dev.gaaus.cobblemonplayershops.menu.ShopBuyMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopViewers;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The only place money and stock change hands. Everything here runs on the server thread and follows
 * validate-then-mutate; nothing trusts client-sent state (the shop always comes from the player's open menu).
 */
public final class ShopTransactions {
	private static final Economy ECONOMY = CobbleDollarsEconomy.INSTANCE;

	private ShopTransactions() {
	}

	private static void tell(ServerPlayer player, String key, Object... args) {
		player.displayClientMessage(Component.translatable("message.cobblemon_player_shops." + key, args), false);
	}

	// ---------------------------------------------------------------- buy

	public static void buy(ServerPlayer player, ShopBuyMenu menu, int offerId, long expectedPrice, int quantity) {
		ShopBlockEntity shop = menu.shop();
		if (shop == null || !shop.stillValid(player)) {
			return;
		}
		ShopConfig config = ShopConfig.get();

		// ---- validate (no mutation) ----
		// 2. offer exists and the price is the one the buyer saw (anti bait-and-switch)
		ShopOffer offer = shop.findOffer(offerId);
		if (offer == null || offer.price() != expectedPrice) {
			tell(player, "offer_changed");
			menu.sendRefresh(player);
			return;
		}
		// 3./4. quantity and overflow-checked totals
		PurchaseMath.Quote quote = PurchaseMath.quote(offer.unitsPerPurchase(), offer.price(), quantity, config.maxPurchaseQuantity, config.maxTransaction);
		if (quote == null) {
			tell(player, "invalid_quantity");
			return;
		}
		ItemStack unit = offer.template().copyWithCount(1);
		// 5. stock
		if (!shop.isAdminShop() && InventoryUtil.countMatching(shop.stock(), unit) < quote.units()) {
			tell(player, "out_of_stock");
			menu.sendRefresh(player);
			return;
		}
		// 6. balance
		BigInteger balance = ECONOMY.getBalance(player);
		if (balance.compareTo(BigInteger.valueOf(quote.total())) < 0) {
			tell(player, "not_enough_money", MoneyFormat.format(quote.total()));
			return;
		}
		// 7. vault capacity
		if (!shop.isAdminShop() && !PurchaseMath.vaultFits(shop.getVault(), quote.total(), config.maxVault)) {
			tell(player, "shop_vault_full");
			return;
		}
		// 8. room in the buyer's inventory
		if (config.requireInventorySpace && !InventoryUtil.canFit(player.getInventory(), unit, quote.units())) {
			tell(player, "inventory_full");
			return;
		}

		// ---- mutate; anything that can still fail happens before money moves ----
		List<ItemStack> given;
		if (shop.isAdminShop()) {
			given = new ArrayList<>();
			for (int size : PurchaseMath.splitStacks(quote.units(), unit.getMaxStackSize())) {
				given.add(unit.copyWithCount(size));
			}
		} else {
			given = InventoryUtil.removeMatching(shop.stock(), unit, quote.units());
			long removed = 0;
			for (ItemStack stack : given) {
				removed += stack.getCount();
			}
			if (removed != quote.units()) {
				InventoryUtil.restore(shop.stock(), given);
				CobblemonPlayerShops.LOGGER.error("[shop-tx] stock mismatch at {} (wanted {}, removed {})", shop.getBlockPos(), quote.units(), removed);
				tell(player, "out_of_stock");
				return;
			}
		}

		if (!ECONOMY.withdraw(player, quote.total())) {
			if (!shop.isAdminShop()) {
				InventoryUtil.restore(shop.stock(), given);
			}
			tell(player, "not_enough_money", MoneyFormat.format(quote.total()));
			return;
		}

		if (!shop.isAdminShop()) {
			shop.addVault(quote.total());
			shop.setChanged();
		}

		for (ItemStack stack : given) {
			player.getInventory().placeItemBackInInventory(stack);
		}

		player.level().playSound(null, shop.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
		tell(player, "bought", quote.units(), unit.getHoverName(), MoneyFormat.format(quote.total()));
		TransactionLog.log("BUY", shop, player, "item=" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(unit.getItem())
			+ " units=" + quote.units() + " total=" + quote.total() + (shop.isAdminShop() ? " admin-sink" : ""));
		ShopViewers.refreshAll(shop);
	}

	// ---------------------------------------------------------------- withdraw

	/** Pays the vault to the player who withdrew (that is what the owner granted). Caller checked the guard. */
	public static void withdraw(ServerPlayer player, ShopBlockEntity shop) {
		if (!shop.stillValid(player) || !shop.canWithdraw(player)) {
			return;
		}
		long amount = shop.takeVault();
		if (amount <= 0) {
			tell(player, "vault_empty");
			return;
		}
		ECONOMY.deposit(player, amount);
		tell(player, "withdrawn", MoneyFormat.format(amount));
		TransactionLog.log("WITHDRAW", shop, player, "amount=" + amount + " role=" + shop.roleOf(player));
		ShopViewers.refreshAll(shop);
	}

	// ---------------------------------------------------------------- payout on removal

	/**
	 * Pays the vault to the shop OWNER (never the breaker). Online owners are credited directly, offline owners
	 * through CobbleDollars' offline earnings. The vault is zeroed before paying so it cannot be paid twice.
	 */
	public static void payoutVault(ShopBlockEntity shop, String reason, @Nullable UUID actor) {
		long amount = shop.takeVault();
		if (amount <= 0) {
			return;
		}
		UUID owner = shop.getOwner();
		var level = shop.getLevel();
		MinecraftServer server = level == null ? null : level.getServer();
		if (owner == null || server == null) {
			CobblemonPlayerShops.LOGGER.error("[shop-tx] LOST {} CobbleDollars: shop at {} has no owner/server ({}). Manual recovery needed.",
				amount, shop.getBlockPos(), reason);
			return;
		}
		ServerPlayer online = server.getPlayerList().getPlayer(owner);
		boolean paid;
		if (online != null) {
			ECONOMY.deposit(online, amount);
			online.sendSystemMessage(Component.translatable("message.cobblemon_player_shops.vault_paid_out", MoneyFormat.format(amount),
				shop.getBlockPos().getX(), shop.getBlockPos().getY(), shop.getBlockPos().getZ()));
			paid = true;
		} else {
			paid = ECONOMY.depositOffline(owner, server, amount);
		}
		if (paid) {
			TransactionLog.logRaw("PAYOUT_" + reason, shop, actor, "amount=" + amount + " to=" + owner + (online != null ? " online" : " offline"));
		} else {
			CobblemonPlayerShops.LOGGER.error("[shop-tx] FAILED to pay {} CobbleDollars to offline owner {} for shop at {} dim={} ({}). Manual recovery needed.",
				amount, owner, shop.getBlockPos(), level.dimension().location(), reason);
		}
	}
}
