package dev.gaaus.cobblemonplayershops.economy;

import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import java.math.BigInteger;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The only class (besides {@link MoneyFormat}) that touches CobbleDollars.
 * Uses getCobbleDollars / setCobbleDollars only; earnCobbleDollars is never called because its
 * income multiplier would create money out of nothing.
 */
public final class CobbleDollarsEconomy implements Economy {
	public static final CobbleDollarsEconomy INSTANCE = new CobbleDollarsEconomy();

	private CobbleDollarsEconomy() {
	}

	@Override
	public BigInteger getBalance(ServerPlayer player) {
		return PlayerExtensionKt.getCobbleDollars(player);
	}

	@Override
	public boolean withdraw(ServerPlayer player, long amount) {
		if (amount < 0) {
			return false;
		}
		BigInteger cost = BigInteger.valueOf(amount);
		BigInteger balance = PlayerExtensionKt.getCobbleDollars(player);
		if (balance.compareTo(cost) < 0) {
			return false;
		}
		PlayerExtensionKt.setCobbleDollars(player, balance.subtract(cost));
		return true;
	}

	@Override
	public void deposit(ServerPlayer player, long amount) {
		if (amount <= 0) {
			return;
		}
		PlayerExtensionKt.setCobbleDollars(player, PlayerExtensionKt.getCobbleDollars(player).add(BigInteger.valueOf(amount)));
	}

	@Override
	public boolean depositOffline(UUID player, MinecraftServer server, long amount) {
		if (amount <= 0) {
			return true;
		}
		return PlayerExtensionKt.addOfflineCobbleDollars(player, server, BigInteger.valueOf(amount));
	}

	/** Display-only balance read, safe on the client (the value is synced via SynchedEntityData). */
	public static BigInteger displayBalance(Player player) {
		return PlayerExtensionKt.getCobbleDollars(player);
	}
}
