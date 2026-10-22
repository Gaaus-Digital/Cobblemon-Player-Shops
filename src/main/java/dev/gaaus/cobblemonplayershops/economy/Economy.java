package dev.gaaus.cobblemonplayershops.economy;

import java.math.BigInteger;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Currency backend. Server-thread only. All amounts are whole, non-negative currency units. */
public interface Economy {
	BigInteger getBalance(ServerPlayer player);

	/** Removes {@code amount} if the player has at least that much. Returns false (and changes nothing) otherwise. */
	boolean withdraw(ServerPlayer player, long amount);

	/** Adds {@code amount} to an online player's balance. */
	void deposit(ServerPlayer player, long amount);

	/** Queues {@code amount} for an offline player. Returns false if it could not be recorded. */
	boolean depositOffline(UUID player, MinecraftServer server, long amount);
}
