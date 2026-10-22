package dev.gaaus.cobblemonplayershops.economy;

import fr.harmex.cobbledollars.common.utils.extensions.BigIntegerExtensionsKt;
import java.math.BigInteger;
import net.minecraft.network.chat.Component;

/** Formats money using CobbleDollars' own formatters, falling back to plain text. */
public final class MoneyFormat {
	private MoneyFormat() {
	}

	/** Full grouped amount, for tooltips and messages. */
	public static Component format(BigInteger amount) {
		try {
			return BigIntegerExtensionsKt.formatFull(amount, true);
		} catch (Throwable t) {
			return Component.literal(String.format("$%,d", amount));
		}
	}

	public static Component format(long amount) {
		return format(BigInteger.valueOf(amount));
	}

	/** Short amount (4K, 1.2M) for price pills; CobbleDollars' formatter already produces K/M/B/T output. */
	public static Component compact(BigInteger amount) {
		try {
			return BigIntegerExtensionsKt.format(amount, true);
		} catch (Throwable t) {
			return Component.literal("$" + CompactNumber.of(amount));
		}
	}

	public static Component compact(long amount) {
		return compact(BigInteger.valueOf(amount));
	}

	/** Compact text for pills that already draw the currency icon, so it must not contain the symbol. */
	public static String compactPlain(BigInteger amount) {
		try {
			return BigIntegerExtensionsKt.format(amount, false).getString();
		} catch (Throwable t) {
			return CompactNumber.of(amount);
		}
	}

	public static String compactPlain(long amount) {
		return compactPlain(BigInteger.valueOf(amount));
	}
}
