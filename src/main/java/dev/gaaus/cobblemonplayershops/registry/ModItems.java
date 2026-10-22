package dev.gaaus.cobblemonplayershops.registry;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.shop.AdminShopBlockItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItems {
	public static final BlockItem PLAYER_SHOP = Registry.register(BuiltInRegistries.ITEM, CobblemonPlayerShops.id("player_shop"),
		new BlockItem(ModBlocks.PLAYER_SHOP, new Item.Properties()));

	public static final AdminShopBlockItem ADMIN_SHOP = Registry.register(BuiltInRegistries.ITEM, CobblemonPlayerShops.id("admin_shop"),
		new AdminShopBlockItem(ModBlocks.ADMIN_SHOP, new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, CobblemonPlayerShops.id("main"));

	private ModItems() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.cobblemon_player_shops"))
			.icon(() -> new ItemStack(PLAYER_SHOP))
			.displayItems((parameters, output) -> {
				output.accept(PLAYER_SHOP);
				output.accept(ADMIN_SHOP);
			})
			.build());
	}
}
