package dev.gaaus.cobblemonplayershops.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Server-authoritative settings (config/cobblemon_player_shops.json). Values are clamped on load. */
public final class ShopConfig {
	/** Absolute upper bounds; keeps every product/sum of config values far away from long overflow. */
	public static final long HARD_MAX_PRICE = 1_000_000_000_000L;
	public static final long HARD_MAX_TRANSACTION = 1_000_000_000_000_000L;
	public static final long HARD_MAX_VAULT = 1_000_000_000_000_000_000L;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile ShopConfig instance = new ShopConfig();

	public long maxPrice = 1_000_000_000L;
	public long maxTransaction = 1_000_000_000_000L;
	public long maxVault = 1_000_000_000_000_000L;
	public int maxOffersPerShop = 24;
	public int maxTrustedPerShop = 8;
	public int maxPurchaseQuantity = 64;
	public boolean requireInventorySpace = true;
	public int rateLimitActions = 10;
	public int rateLimitWindowTicks = 20;
	public double interactionRange = 8.0;
	public boolean logTransactions = true;

	public static ShopConfig get() {
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("cobblemon_player_shops.json");
	}

	/** Reads the config (writing the clamped result back). Never throws; a broken file falls back to defaults. */
	public static void load() {
		Path path = path();
		ShopConfig loaded = null;
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, ShopConfig.class);
			} catch (IOException | RuntimeException e) {
				CobblemonPlayerShops.LOGGER.error("Could not read {}, using defaults", path, e);
			}
		}
		if (loaded == null) {
			loaded = new ShopConfig();
		}
		loaded.clamp();
		instance = loaded;
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(loaded, writer);
		} catch (IOException e) {
			CobblemonPlayerShops.LOGGER.warn("Could not write {}", path, e);
		}
	}

	public void clamp() {
		maxPrice = clamp(maxPrice, 1, HARD_MAX_PRICE);
		maxTransaction = clamp(maxTransaction, 1, HARD_MAX_TRANSACTION);
		maxVault = clamp(maxVault, 1, HARD_MAX_VAULT);
		maxOffersPerShop = (int) clamp(maxOffersPerShop, 1, 54);
		maxTrustedPerShop = (int) clamp(maxTrustedPerShop, 0, 32);
		maxPurchaseQuantity = (int) clamp(maxPurchaseQuantity, 1, 2304);
		rateLimitActions = (int) clamp(rateLimitActions, 1, 200);
		rateLimitWindowTicks = (int) clamp(rateLimitWindowTicks, 1, 200);
		if (Double.isNaN(interactionRange)) {
			interactionRange = 8.0;
		}
		interactionRange = Math.max(2.0, Math.min(64.0, interactionRange));
	}

	private static long clamp(long value, long min, long max) {
		return Math.max(min, Math.min(max, value));
	}
}
