package dev.gaaus.cobblemonplayershops.menu;

import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.shop.InventoryUtil;
import dev.gaaus.cobblemonplayershops.shop.ShopAccess;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import dev.gaaus.cobblemonplayershops.shop.ShopOffer;
import dev.gaaus.cobblemonplayershops.shop.ShopRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a shop screen needs, sent on open and on every refresh. Flags are display-only: the server
 * re-checks roles live on every packet and slot operation. Stock counts are per viewer-visible offer only;
 * the vault balance is sent to every manager of a player shop (withdrawing still needs the withdraw permission) and the trusted list only to the owner.
 */
public record ShopMenuData(
	boolean admin,
	ShopRole role,
	boolean canAccessInventory,
	boolean canWithdraw,
	boolean canEditOffers,
	boolean canManageAccess,
	boolean hopperInput,
	String ownerName,
	long vault,
	ItemStack template,
	List<OfferView> offers,
	List<TrustedView> trusted,
	int maxOffers,
	int maxTrusted,
	long maxPrice,
	int maxQuantity
) {
	/** {@code available} is in single units; -1 means unlimited (admin shop). */
	public record OfferView(int id, ItemStack template, long price, long available) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OfferView> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, OfferView::id,
			ItemStack.STREAM_CODEC, OfferView::template,
			ByteBufCodecs.VAR_LONG, OfferView::price,
			ByteBufCodecs.VAR_LONG, OfferView::available,
			OfferView::new);

		public int unitsPerPurchase() {
			return template.getCount();
		}

		/** How many purchases of this offer the shop can currently serve. */
		public long availablePurchases() {
			return available < 0 ? Long.MAX_VALUE : available / Math.max(1, template.getCount());
		}
	}

	public record TrustedView(UUID id, String name, boolean canAccessInventory, boolean canWithdraw) {
		public static final StreamCodec<RegistryFriendlyByteBuf, TrustedView> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, TrustedView::id,
			ByteBufCodecs.stringUtf8(16), TrustedView::name,
			ByteBufCodecs.BOOL, TrustedView::canAccessInventory,
			ByteBufCodecs.BOOL, TrustedView::canWithdraw,
			TrustedView::new);
	}

	private static final StreamCodec<RegistryFriendlyByteBuf, List<OfferView>> OFFERS_CODEC = OfferView.STREAM_CODEC.apply(ByteBufCodecs.list(64));
	private static final StreamCodec<RegistryFriendlyByteBuf, List<TrustedView>> TRUSTED_CODEC = TrustedView.STREAM_CODEC.apply(ByteBufCodecs.list(64));

	public static final StreamCodec<RegistryFriendlyByteBuf, ShopMenuData> STREAM_CODEC = StreamCodec.of(ShopMenuData::write, ShopMenuData::read);

	private static void write(RegistryFriendlyByteBuf buf, ShopMenuData d) {
		buf.writeBoolean(d.admin);
		buf.writeVarInt(d.role.ordinal());
		buf.writeBoolean(d.canAccessInventory);
		buf.writeBoolean(d.canWithdraw);
		buf.writeBoolean(d.canEditOffers);
		buf.writeBoolean(d.canManageAccess);
		buf.writeBoolean(d.hopperInput);
		buf.writeUtf(d.ownerName, 64);
		buf.writeVarLong(d.vault);
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, d.template);
		OFFERS_CODEC.encode(buf, d.offers);
		TRUSTED_CODEC.encode(buf, d.trusted);
		buf.writeVarInt(d.maxOffers);
		buf.writeVarInt(d.maxTrusted);
		buf.writeVarLong(d.maxPrice);
		buf.writeVarInt(d.maxQuantity);
	}

	private static ShopMenuData read(RegistryFriendlyByteBuf buf) {
		boolean admin = buf.readBoolean();
		ShopRole[] roles = ShopRole.values();
		int roleIndex = buf.readVarInt();
		ShopRole role = roleIndex >= 0 && roleIndex < roles.length ? roles[roleIndex] : ShopRole.CUSTOMER;
		boolean inv = buf.readBoolean();
		boolean withdraw = buf.readBoolean();
		boolean edit = buf.readBoolean();
		boolean manage = buf.readBoolean();
		boolean hopper = buf.readBoolean();
		String owner = buf.readUtf(64);
		long vault = buf.readVarLong();
		ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
		List<OfferView> offers = OFFERS_CODEC.decode(buf);
		List<TrustedView> trusted = TRUSTED_CODEC.decode(buf);
		int maxOffers = buf.readVarInt();
		int maxTrusted = buf.readVarInt();
		long maxPrice = buf.readVarLong();
		int maxQuantity = buf.readVarInt();
		return new ShopMenuData(admin, role, inv, withdraw, edit, manage, hopper, owner, vault, template, offers, trusted,
			maxOffers, maxTrusted, maxPrice, maxQuantity);
	}

	/** Builds the view for {@code viewer}. Server side only. */
	public static ShopMenuData create(ShopBlockEntity shop, Player viewer, ItemStack templateBuffer) {
		ShopConfig config = ShopConfig.get();
		boolean canWithdraw = shop.canWithdraw(viewer);
		boolean canManage = shop.canManageAccess(viewer);

		List<OfferView> offers = new ArrayList<>();
		for (ShopOffer offer : shop.getOffers()) {
			long available = shop.isAdminShop() ? -1 : InventoryUtil.countMatching(shop.stock(), offer.template());
			offers.add(new OfferView(offer.id(), offer.template(), offer.price(), available));
		}

		List<TrustedView> trusted = new ArrayList<>();
		if (canManage) {
			for (Map.Entry<UUID, ShopAccess> entry : shop.getTrusted().entrySet()) {
				ShopAccess access = entry.getValue();
				trusted.add(new TrustedView(entry.getKey(), access.lastKnownName(), access.canAccessInventory(), access.canWithdraw()));
			}
		}

		return new ShopMenuData(
			shop.isAdminShop(),
			shop.roleOf(viewer),
			shop.canAccessInventory(viewer),
			canWithdraw,
			shop.canEditOffers(viewer),
			canManage,
			shop.isHopperInput(),
			shop.getOwnerName(),
			!shop.isAdminShop() && shop.roleOf(viewer).isManager() ? shop.getVault() : 0,
			templateBuffer.copy(),
			offers,
			trusted,
			config.maxOffersPerShop,
			config.maxTrustedPerShop,
			config.maxPrice,
			config.maxPurchaseQuantity);
	}
}
