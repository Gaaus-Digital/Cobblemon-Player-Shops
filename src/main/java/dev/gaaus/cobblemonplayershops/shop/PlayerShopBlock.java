package dev.gaaus.cobblemonplayershops.shop;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/**
 * Craftable shop. Deliberately an ordinary breakable block (no explosion immunity, no wither/dragon
 * immune tags): grief protection is the job of claim mods, and the vault is always paid to the owner.
 */
public class PlayerShopBlock extends ShopBlock {
	public static final MapCodec<PlayerShopBlock> CODEC = simpleCodec(PlayerShopBlock::new);

	public PlayerShopBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean isAdminShop() {
		return false;
	}
}
