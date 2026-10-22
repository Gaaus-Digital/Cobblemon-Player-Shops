package dev.gaaus.cobblemonplayershops.registry;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
	/** Items that can never be offered for sale. Datapack-extendable. */
	public static final TagKey<Item> UNSELLABLE = TagKey.create(Registries.ITEM, CobblemonPlayerShops.id("unsellable"));

	private ModTags() {
	}
}
