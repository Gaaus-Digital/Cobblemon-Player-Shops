package dev.gaaus.cobblemonplayershops;

import dev.gaaus.cobblemonplayershops.command.ShopCommand;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.network.ModPayloads;
import dev.gaaus.cobblemonplayershops.network.RateLimiter;
import dev.gaaus.cobblemonplayershops.permission.Perms;
import dev.gaaus.cobblemonplayershops.registry.ModBlockEntities;
import dev.gaaus.cobblemonplayershops.registry.ModBlocks;
import dev.gaaus.cobblemonplayershops.registry.ModItems;
import dev.gaaus.cobblemonplayershops.registry.ModMenus;
import dev.gaaus.cobblemonplayershops.shop.AdminShopBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblemonPlayerShops implements ModInitializer {
	public static final String MOD_ID = "cobblemon_player_shops";
	public static final Logger LOGGER = LoggerFactory.getLogger("CobblemonPlayerShops");

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ShopConfig.load();

		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		ModMenus.init();
		ModPayloads.register();

		// Admin shop: only admins may break it (it is unbreakable in survival; this also covers creative).
		// Player shops intentionally have no veto: claim mods handle protection, and the vault always goes to the owner.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			if (state.getBlock() instanceof AdminShopBlock && !Perms.isAdmin(player)) {
				player.displayClientMessage(Component.translatable("message.cobblemon_player_shops.admin_shop_denied"), true);
				return false;
			}
			return true;
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RateLimiter.forget(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> RateLimiter.clear());

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ShopCommand.register(dispatcher));
		LOGGER.info("Cobblemon: Player Shops initialised");
	}
}
