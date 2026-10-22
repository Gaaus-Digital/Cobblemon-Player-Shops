package dev.gaaus.cobblemonplayershops.network;

import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Per-player fixed-window limiter (config: rateLimitActions per rateLimitWindowTicks). Server thread only. */
public final class RateLimiter {
	private static final class Window {
		long start;
		int count;
	}

	private static final Map<UUID, Window> WINDOWS = new HashMap<>();

	private RateLimiter() {
	}

	/** True if the action may proceed. Excess actions are dropped silently by the caller. */
	public static boolean tryAcquire(ServerPlayer player) {
		ShopConfig config = ShopConfig.get();
		long now = player.server.getTickCount();
		Window window = WINDOWS.computeIfAbsent(player.getUUID(), id -> new Window());
		if (now - window.start >= config.rateLimitWindowTicks || now < window.start) {
			window.start = now;
			window.count = 0;
		}
		if (window.count >= config.rateLimitActions) {
			return false;
		}
		window.count++;
		return true;
	}

	public static void forget(UUID player) {
		WINDOWS.remove(player);
	}

	public static void clear() {
		WINDOWS.clear();
	}
}
