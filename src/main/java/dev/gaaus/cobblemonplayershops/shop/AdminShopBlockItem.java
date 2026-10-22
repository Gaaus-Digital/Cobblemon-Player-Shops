package dev.gaaus.cobblemonplayershops.shop;

import dev.gaaus.cobblemonplayershops.permission.Perms;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/** Refuses placement unless the placer has the admin permission (checked on the server). */
public class AdminShopBlockItem extends BlockItem {
	public AdminShopBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.FAIL;
		}
		if (!context.getLevel().isClientSide() && !Perms.isAdmin(player)) {
			player.displayClientMessage(Component.translatable("message.cobblemon_player_shops.admin_shop_denied"), true);
			return InteractionResult.FAIL;
		}
		return super.place(context);
	}
}
