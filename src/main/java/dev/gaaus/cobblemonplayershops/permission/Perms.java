package dev.gaaus.cobblemonplayershops.permission;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

/** fabric-permissions-api (LuckPerms etc.) with an op-level fallback. */
public final class Perms {
	public static final String ADMIN = "cobblemon_player_shops.admin";
	public static final String COMMAND_RELOAD = "cobblemon_player_shops.command.reload";
	public static final String COMMAND_INFO = "cobblemon_player_shops.command.info";

	private Perms() {
	}

	public static boolean isAdmin(Player player) {
		return Permissions.check(player, ADMIN, 2);
	}

	public static boolean check(CommandSourceStack source, String node, int defaultLevel) {
		return Permissions.check(source, node, defaultLevel);
	}
}
