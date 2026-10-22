package dev.gaaus.cobblemonplayershops.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.permission.Perms;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** /playershops (alias /ps) reload | info (moderation helpers). */
public final class ShopCommand {
	private ShopCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("playershops")
			.then(Commands.literal("reload")
				.requires(s -> Perms.check(s, Perms.COMMAND_RELOAD, 2))
				.executes(ctx -> {
					ShopConfig.load();
					ctx.getSource().sendSuccess(() -> Component.translatable("command.cobblemon_player_shops.reloaded"), true);
					return 1;
				}))
			.then(Commands.literal("info")
				.requires(s -> Perms.check(s, Perms.COMMAND_INFO, 2))
				.executes(ctx -> info(ctx.getSource()))));
		dispatcher.register(Commands.literal("ps").redirect(root));
	}

	private static int info(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		HitResult hit = player.pick(8.0, 0.0f, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
			|| !(player.level().getBlockEntity(blockHit.getBlockPos()) instanceof ShopBlockEntity shop)) {
			source.sendFailure(Component.translatable("command.cobblemon_player_shops.no_shop"));
			return 0;
		}
		BlockPos pos = shop.getBlockPos();
		source.sendSuccess(() -> Component.translatable("command.cobblemon_player_shops.info.header", pos.getX(), pos.getY(), pos.getZ(),
			Component.translatable(shop.isAdminShop() ? "command.cobblemon_player_shops.info.kind_admin" : "command.cobblemon_player_shops.info.kind_player")), false);
		if (shop.getOwner() != null) {
			source.sendSuccess(() -> Component.translatable("command.cobblemon_player_shops.info.owner", shop.getOwnerName(), shop.getOwner().toString()), false);
		} else {
			source.sendSuccess(() -> Component.translatable("command.cobblemon_player_shops.info.no_owner"), false);
		}
		source.sendSuccess(() -> Component.translatable("command.cobblemon_player_shops.info.vault", MoneyFormat.format(shop.getVault())), false);
		source.sendSuccess(() -> Component.translatable("command.cobblemon_player_shops.info.offers", shop.getOffers().size(), shop.getTrusted().size()), false);
		return 1;
	}
}
