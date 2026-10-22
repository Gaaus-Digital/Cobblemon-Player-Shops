package dev.gaaus.cobblemonplayershops.registry;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<ShopBlockEntity> SHOP = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CobblemonPlayerShops.id("shop"),
		BlockEntityType.Builder.of(ShopBlockEntity::new, ModBlocks.PLAYER_SHOP, ModBlocks.ADMIN_SHOP).build(null));

	private ModBlockEntities() {
	}

	public static void init() {
		// class load registers the block entity type
	}
}
