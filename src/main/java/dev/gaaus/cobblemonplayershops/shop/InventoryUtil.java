package dev.gaaus.cobblemonplayershops.shop;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class InventoryUtil {
	private InventoryUtil() {
	}

	public static long countMatching(List<ItemStack> stacks, ItemStack template) {
		long count = 0;
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/**
	 * Removes exactly {@code units} matching items from {@code stacks} and returns them as real stacks.
	 * If fewer are available nothing is removed and an empty list is returned.
	 */
	public static List<ItemStack> removeMatching(NonNullList<ItemStack> stacks, ItemStack template, long units) {
		List<ItemStack> removed = new ArrayList<>();
		if (units < 1 || countMatching(stacks, template) < units) {
			return removed;
		}
		long left = units;
		for (int i = 0; i < stacks.size() && left > 0; i++) {
			ItemStack stack = stacks.get(i);
			if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) {
				continue;
			}
			int take = (int) Math.min(left, stack.getCount());
			removed.add(stack.split(take));
			left -= take;
		}
		return removed;
	}

	/** Puts previously removed stacks back (undo of a failed purchase). Merges first, then fills empty slots. */
	public static void restore(NonNullList<ItemStack> stacks, List<ItemStack> removed) {
		for (ItemStack back : removed) {
			ItemStack rest = back.copy();
			for (int i = 0; i < stacks.size() && !rest.isEmpty(); i++) {
				ItemStack slot = stacks.get(i);
				if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
					int move = Math.min(slot.getMaxStackSize() - slot.getCount(), rest.getCount());
					if (move > 0) {
						slot.grow(move);
						rest.shrink(move);
					}
				}
			}
			for (int i = 0; i < stacks.size() && !rest.isEmpty(); i++) {
				if (stacks.get(i).isEmpty()) {
					stacks.set(i, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
				}
			}
		}
	}

	/** Can the player's 36 main slots absorb {@code units} items of {@code template}? */
	public static boolean canFit(Inventory inventory, ItemStack template, long units) {
		return canFit(inventory.items, template, units);
	}

	/** Slot-list variant of {@link #canFit(Inventory, ItemStack, long)} (pure; used by tests). */
	public static boolean canFit(List<ItemStack> slots, ItemStack template, long units) {
		long room = 0;
		int max = template.getMaxStackSize();
		for (ItemStack slot : slots) {
			if (slot.isEmpty()) {
				room += max;
			} else if (ItemStack.isSameItemSameComponents(slot, template)) {
				room += Math.max(0, slot.getMaxStackSize() - slot.getCount());
			}
			if (room >= units) {
				return true;
			}
		}
		return false;
	}
}
