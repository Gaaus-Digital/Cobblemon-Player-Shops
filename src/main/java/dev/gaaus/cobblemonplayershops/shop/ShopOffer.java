package dev.gaaus.cobblemonplayershops.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * One thing a shop sells. {@code template} is a ghost copy (never a real item); its count is the
 * number of units handed out per purchase. Every add or edit gets a fresh id, so a stale buy
 * packet can never hit an offer that was changed in the meantime.
 */
public record ShopOffer(int id, ItemStack template, long price) {
	public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("id").forGetter(ShopOffer::id),
		ItemStack.CODEC.fieldOf("item").forGetter(o -> o.template),
		Codec.LONG.fieldOf("price").forGetter(ShopOffer::price)
	).apply(i, ShopOffer::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ShopOffer> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ShopOffer::id,
		ItemStack.STREAM_CODEC, o -> o.template,
		ByteBufCodecs.VAR_LONG, ShopOffer::price,
		ShopOffer::new);

	/**
	 * Validates against the hard bounds only. The (lower, configurable) maxPrice is enforced when an
	 * offer is saved, so lowering the config never silently deletes existing offers on load.
	 */
	public ShopOffer {
		if (template.isEmpty()) {
			throw new IllegalArgumentException("Offer template must not be empty");
		}
		if (template.getCount() < 1 || template.getCount() > template.getMaxStackSize()) {
			throw new IllegalArgumentException("Offer unit count out of range");
		}
		if (price < 1 || price > ShopConfig.HARD_MAX_PRICE) {
			throw new IllegalArgumentException("Offer price out of range");
		}
		template = template.copy();
	}

	/** A defensive copy; the stored template is never exposed. */
	@Override
	public ItemStack template() {
		return template.copy();
	}

	public int unitsPerPurchase() {
		return template.getCount();
	}

	public boolean matchesTemplate(ItemStack other) {
		return ItemStack.isSameItemSameComponents(template, other);
	}
}
