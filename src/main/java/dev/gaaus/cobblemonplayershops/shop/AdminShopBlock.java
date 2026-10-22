package dev.gaaus.cobblemonplayershops.shop;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;

/** Inexhaustible shop: infinite stock, money spent is destroyed. Not craftable. */
public class AdminShopBlock extends ShopBlock {
	public static final MapCodec<AdminShopBlock> CODEC = simpleCodec(AdminShopBlock::new);

	public AdminShopBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean isAdminShop() {
		return true;
	}
}
