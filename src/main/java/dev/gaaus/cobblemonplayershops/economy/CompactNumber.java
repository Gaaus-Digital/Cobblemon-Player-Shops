package dev.gaaus.cobblemonplayershops.economy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/** Pure-Java compact number text (4K, 1.2M, 9.75M): at most 3 significant digits, rounded down, no trailing zeros. */
public final class CompactNumber {
	private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};
	private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

	private CompactNumber() {
	}

	public static String of(BigInteger amount) {
		BigDecimal value = new BigDecimal(amount);
		int tier = 0;
		while (value.abs().compareTo(THOUSAND) >= 0 && tier < SUFFIXES.length - 1) {
			value = value.divide(THOUSAND);
			tier++;
		}
		if (tier == 0) {
			return value.toBigInteger().toString();
		}
		int integerDigits = value.toBigInteger().abs().toString().length();
		value = value.setScale(Math.max(0, 3 - integerDigits), RoundingMode.DOWN).stripTrailingZeros();
		return value.toPlainString() + SUFFIXES[tier];
	}
}
