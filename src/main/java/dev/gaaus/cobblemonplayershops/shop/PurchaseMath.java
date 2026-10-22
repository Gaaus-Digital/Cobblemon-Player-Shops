package dev.gaaus.cobblemonplayershops.shop;

/** Pure overflow-checked money/quantity math. No Minecraft types so it is unit-testable. */
public final class PurchaseMath {
	private PurchaseMath() {
	}

	public record Quote(long units, long total) {
	}

	/**
	 * @return the quote, or {@code null} if the request is invalid (bad numbers, overflow, over the caps)
	 */
	public static Quote quote(int unitsPerPurchase, long price, int quantity, int maxQuantity, long maxTransaction) {
		if (unitsPerPurchase < 1 || price < 1 || quantity < 1 || quantity > maxQuantity) {
			return null;
		}
		try {
			long units = Math.multiplyExact((long) unitsPerPurchase, (long) quantity);
			long total = Math.multiplyExact(price, (long) quantity);
			if (total > maxTransaction) {
				return null;
			}
			return new Quote(units, total);
		} catch (ArithmeticException e) {
			return null;
		}
	}

	/** True when {@code vault + add} stays within {@code maxVault} (and does not overflow). */
	public static boolean vaultFits(long vault, long add, long maxVault) {
		if (vault < 0 || add < 0) {
			return false;
		}
		try {
			return Math.addExact(vault, add) <= maxVault;
		} catch (ArithmeticException e) {
			return false;
		}
	}

	/** Splits {@code units} into stack sizes of at most {@code maxStack}. */
	public static int[] splitStacks(long units, int maxStack) {
		if (units < 0 || maxStack < 1 || units > (long) maxStack * 4096) {
			throw new IllegalArgumentException("bad split");
		}
		int full = (int) (units / maxStack);
		int rest = (int) (units % maxStack);
		int[] out = new int[full + (rest > 0 ? 1 : 0)];
		for (int i = 0; i < full; i++) {
			out[i] = maxStack;
		}
		if (rest > 0) {
			out[full] = rest;
		}
		return out;
	}
}
