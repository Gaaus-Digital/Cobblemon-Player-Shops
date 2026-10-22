package dev.gaaus.cobblemonplayershops.registry;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.shop.AdminShopBlock;
import dev.gaaus.cobblemonplayershops.shop.PlayerShopBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	/** Ordinary stone-like hardness and blast resistance; no immunity of any kind. */
	public static final PlayerShopBlock PLAYER_SHOP = Registry.register(BuiltInRegistries.BLOCK, CobblemonPlayerShops.id("player_shop"),
		new PlayerShopBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.sound(SoundType.WOOD)
			.strength(5.0f, 6.0f)
			.noOcclusion()
			.pushReaction(PushReaction.BLOCK)));

	/** Unbreakable in survival; only admins may break it in creative (see ModEvents). */
	public static final AdminShopBlock ADMIN_SHOP = Registry.register(BuiltInRegistries.BLOCK, CobblemonPlayerShops.id("admin_shop"),
		new AdminShopBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.GOLD)
			.sound(SoundType.WOOD)
			.strength(-1.0f, 3_600_000.0f)
			.noLootTable()
			.noOcclusion()
			.pushReaction(PushReaction.BLOCK)));

	private ModBlocks() {
	}

	public static void init() {
		// class load registers the blocks
	}
}
