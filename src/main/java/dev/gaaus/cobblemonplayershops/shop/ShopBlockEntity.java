package dev.gaaus.cobblemonplayershops.shop;

import com.mojang.serialization.Codec;
import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.config.ShopConfig;
import dev.gaaus.cobblemonplayershops.menu.ShopMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopViewers;
import dev.gaaus.cobblemonplayershops.permission.Perms;
import dev.gaaus.cobblemonplayershops.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * State of one shop: stock, offers, vault, owner and trusted players. Everything money-related that
 * mutates this goes through {@link ShopTransactions}.
 */
public class ShopBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int SIZE = 36;
	public static final int MAX_NAME_LENGTH = 32;

	private static final int[] ALL_SLOTS = IntStream.range(0, SIZE).toArray();
	private static final int[] NO_SLOTS = new int[0];
	private static final Codec<Map<UUID, ShopAccess>> TRUSTED_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, ShopAccess.CODEC);

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private final List<ShopOffer> offers = new ArrayList<>();
	private int nextOfferId = 1;
	private long vault;
	@Nullable
	private UUID owner;
	private String ownerName = "";
	private boolean hopperInput;
	private final boolean admin;
	private final LinkedHashMap<UUID, ShopAccess> trusted = new LinkedHashMap<>();
	@Nullable
	private String shopName;

	/** Who started breaking this block (log line only; payout never depends on it). */
	@Nullable
	private UUID lastBreaker;
	@Nullable
	private String lastBreakerName;

	/** Client-side only: which offer the renderer is currently showing. */
	private int tradeIndex;

	/** Server-side: something changed that open menus should see; flushed once per tick. */
	private boolean viewersDirty;

	public ShopBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SHOP, pos, state);
		this.admin = state.getBlock() instanceof ShopBlock shop && shop.isAdminShop();
	}

	// ---------------------------------------------------------------- identity & roles

	public boolean isAdminShop() {
		return admin;
	}

	@Nullable
	public UUID getOwner() {
		return owner;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwner(@Nullable UUID owner, String ownerName) {
		this.owner = owner;
		this.ownerName = ownerName;
		setChanged();
	}

	/** Single source of truth for what a player may do. Computed live, never cached. */
	public ShopRole roleOf(Player player) {
		if (admin) {
			return Perms.isAdmin(player) ? ShopRole.ADMIN : ShopRole.CUSTOMER;
		}
		UUID id = player.getUUID();
		if (owner != null && owner.equals(id)) {
			return ShopRole.OWNER;
		}
		ShopAccess access = trusted.get(id);
		if (access != null && access.hasAnyAccess()) {
			return ShopRole.TRUSTED;
		}
		if (owner == null && Perms.isAdmin(player)) {
			return ShopRole.ADMIN;
		}
		return ShopRole.CUSTOMER;
	}

	public boolean canEditOffers(Player player) {
		ShopRole role = roleOf(player);
		return role == ShopRole.OWNER || role == ShopRole.ADMIN;
	}

	public boolean canAccessInventory(Player player) {
		if (admin) {
			return false;
		}
		ShopRole role = roleOf(player);
		if (role == ShopRole.OWNER) {
			return true;
		}
		ShopAccess access = trusted.get(player.getUUID());
		return role == ShopRole.TRUSTED && access != null && access.canAccessInventory();
	}

	public boolean canWithdraw(Player player) {
		if (admin) {
			return false;
		}
		ShopRole role = roleOf(player);
		if (role == ShopRole.OWNER) {
			return true;
		}
		ShopAccess access = trusted.get(player.getUUID());
		return role == ShopRole.TRUSTED && access != null && access.canWithdraw();
	}

	/** Original owner only. Admins and ops deliberately do not bypass this. */
	public boolean canManageAccess(Player player) {
		return !admin && owner != null && owner.equals(player.getUUID());
	}

	// ---------------------------------------------------------------- vault

	public long getVault() {
		return vault;
	}

	/** Callers must have validated the cap with {@link PurchaseMath#vaultFits}; overflow still throws. */
	public void addVault(long amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("negative vault add");
		}
		vault = Math.addExact(vault, amount);
		setChanged();
	}

	/** Returns the vault contents and zeroes it first, so it can never be paid out twice. */
	public long takeVault() {
		long amount = vault;
		vault = 0;
		setChanged();
		return amount;
	}

	// ---------------------------------------------------------------- offers

	public List<ShopOffer> getOffers() {
		return Collections.unmodifiableList(offers);
	}

	@Nullable
	public ShopOffer findOffer(int id) {
		for (ShopOffer offer : offers) {
			if (offer.id() == id) {
				return offer;
			}
		}
		return null;
	}

	/**
	 * Adds or replaces an offer. {@code replaceId} (or, failing that, an existing offer for the same item with the
	 * same unit count) is replaced in place; the result always gets a brand-new id.
	 *
	 * @return the stored offer, or null if the shop is full
	 */
	@Nullable
	public ShopOffer saveOffer(int replaceId, ItemStack template, long price) {
		int index = -1;
		for (int i = 0; i < offers.size(); i++) {
			if (offers.get(i).id() == replaceId) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			for (int i = 0; i < offers.size(); i++) {
				ShopOffer other = offers.get(i);
				if (other.matchesTemplate(template) && other.unitsPerPurchase() == template.getCount()) {
					index = i;
					break;
				}
			}
		}
		if (index == -1 && offers.size() >= ShopConfig.get().maxOffersPerShop) {
			return null;
		}
		ShopOffer offer = new ShopOffer(nextOfferId++, template, price);
		if (index == -1) {
			offers.add(offer);
		} else {
			offers.set(index, offer);
		}
		syncToClients();
		return offer;
	}

	public boolean removeOffer(int id) {
		boolean removed = offers.removeIf(o -> o.id() == id);
		if (removed) {
			syncToClients();
		}
		return removed;
	}

	// ---------------------------------------------------------------- trusted players

	public Map<UUID, ShopAccess> getTrusted() {
		return Collections.unmodifiableMap(trusted);
	}

	public void putTrusted(UUID id, ShopAccess access) {
		trusted.put(id, access);
		setChanged();
	}

	public boolean removeTrusted(UUID id) {
		boolean removed = trusted.remove(id) != null;
		if (removed) {
			setChanged();
		}
		return removed;
	}

	/** Keeps the display name fresh; identity is always the UUID. */
	public void refreshTrustedName(Player player) {
		ShopAccess access = trusted.get(player.getUUID());
		String name = player.getGameProfile().getName();
		if (access != null && !access.lastKnownName().equals(name)) {
			trusted.put(player.getUUID(), new ShopAccess(name, access.canAccessInventory(), access.canWithdraw()));
			setChanged();
		}
	}

	// ---------------------------------------------------------------- misc state

	public boolean isHopperInput() {
		return hopperInput && !admin;
	}

	public void setHopperInput(boolean value) {
		this.hopperInput = value && !admin;
		setChanged();
	}

	public NonNullList<ItemStack> stock() {
		return items;
	}

	public void noteBreaker(Player player) {
		this.lastBreaker = player.getUUID();
		this.lastBreakerName = player.getGameProfile().getName();
	}

	@Nullable
	public UUID getLastBreaker() {
		return lastBreaker;
	}

	/**
	 * Called when a player places this shop. Wipes everything a copied block-item NBT could have injected
	 * (vault, trusted list) and, unless the placer is an admin, offers and stock as well.
	 */
	public void initializeForPlacement(ServerPlayer placer, boolean keepContents) {
		this.owner = placer.getUUID();
		this.ownerName = placer.getGameProfile().getName();
		this.vault = 0;
		this.trusted.clear();
		if (!keepContents) {
			this.offers.clear();
			this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
			this.hopperInput = false;
		}
		if (admin) {
			this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
			this.hopperInput = false;
		}
		setChanged();
		syncToClients();
	}

	/** Pushes offers / name to tracking clients (not called for plain stock changes). */
	public void syncToClients() {
		setChanged();
		if (level != null && !level.isClientSide) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	public void closeAllViewers() {
		if (level == null || level.isClientSide || level.getServer() == null) {
			return;
		}
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			if (player.containerMenu instanceof ShopMenu menu && menu.shop() == this) {
				player.closeContainer();
			}
		}
	}

	// ---------------------------------------------------------------- naming

	@Override
	protected Component getDefaultName() {
		return Component.translatable(admin ? "container.cobblemon_player_shops.admin_shop" : "container.cobblemon_player_shops.player_shop");
	}

	@Override
	public Component getName() {
		return shopName != null ? Component.literal(shopName) : getDefaultName();
	}

	@Nullable
	@Override
	public Component getCustomName() {
		return shopName != null ? Component.literal(shopName) : null;
	}

	public void setShopName(@Nullable String name) {
		if (name == null || name.isBlank()) {
			this.shopName = null;
		} else {
			this.shopName = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		// Deliberately not calling super: it would store an unbounded name and honour the LOCK component.
		Component custom = input.get(DataComponents.CUSTOM_NAME);
		setShopName(custom == null ? null : custom.getString());
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder builder) {
		if (shopName != null) {
			builder.set(DataComponents.CUSTOM_NAME, Component.literal(shopName));
		}
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		super.removeComponentsFromTag(tag);
		tag.remove("ShopName");
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null && !level.isClientSide) {
			viewersDirty = true;
		}
	}

	/** Server ticker: coalesces any number of changes into one refresh per tick for open menus. */
	public static void serverTick(Level level, BlockPos pos, BlockState state, ShopBlockEntity shop) {
		if (shop.viewersDirty) {
			shop.viewersDirty = false;
			ShopViewers.refreshAll(shop);
		}
	}

	// ---------------------------------------------------------------- Container / WorldlyContainer

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return isHopperInput() ? ALL_SLOTS : NO_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		return isHopperInput();
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return ShopProtection.canStillUse(player, this);
	}

	/** Never used: menus are opened through the block's own factory, not through MenuProvider. */
	@Nullable
	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return null;
	}

	@Override
	public boolean onlyOpCanSetNbt() {
		return true;
	}

	// ---------------------------------------------------------------- NBT & sync

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
		tag.put("Offers", encodeOffers(registries));
		tag.putInt("NextOfferId", nextOfferId);
		tag.putLong("Vault", vault);
		if (owner != null) {
			tag.putUUID("Owner", owner);
		}
		tag.putString("OwnerName", ownerName);
		tag.putBoolean("HopperInput", hopperInput);
		RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
		TRUSTED_CODEC.encodeStart(ops, trusted).resultOrPartial(err -> CobblemonPlayerShops.LOGGER.error("Could not save trusted list at {}: {}", worldPosition, err))
			.ifPresent(t -> tag.put("Trusted", t));
		if (shopName != null) {
			tag.putString("ShopName", shopName);
		}
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(tag, items, registries);

		offers.clear();
		int maxId = 0;
		if (tag.contains("Offers", Tag.TAG_LIST)) {
			RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
			for (Tag element : tag.getList("Offers", Tag.TAG_COMPOUND)) {
				try {
					ShopOffer.CODEC.parse(ops, element)
						.resultOrPartial(err -> CobblemonPlayerShops.LOGGER.error("Dropping unreadable offer at {}: {}", worldPosition, err))
						.ifPresent(offer -> offers.add(offer));
				} catch (RuntimeException e) {
					CobblemonPlayerShops.LOGGER.error("Dropping invalid offer at {}", worldPosition, e);
				}
			}
			for (ShopOffer offer : offers) {
				maxId = Math.max(maxId, offer.id());
			}
		}
		nextOfferId = Math.max(Math.max(tag.getInt("NextOfferId"), maxId + 1), 1);

		vault = Math.max(0, tag.getLong("Vault"));
		owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		ownerName = tag.getString("OwnerName");
		hopperInput = tag.getBoolean("HopperInput");
		shopName = tag.contains("ShopName", Tag.TAG_STRING) ? tag.getString("ShopName") : null;

		trusted.clear();
		if (tag.contains("Trusted")) {
			RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
			try {
				TRUSTED_CODEC.parse(ops, tag.get("Trusted"))
					.resultOrPartial(err -> CobblemonPlayerShops.LOGGER.error("Dropping unreadable trusted list at {}: {}", worldPosition, err))
					.ifPresent(trusted::putAll);
			} catch (RuntimeException e) {
				CobblemonPlayerShops.LOGGER.error("Dropping invalid trusted list at {}", worldPosition, e);
			}
		}
	}

	private ListTag encodeOffers(HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
		for (ShopOffer offer : offers) {
			ShopOffer.CODEC.encodeStart(ops, offer)
				.resultOrPartial(err -> CobblemonPlayerShops.LOGGER.error("Could not save offer at {}: {}", worldPosition, err))
				.ifPresent(list::add);
		}
		return list;
	}

	/** Only what a client needs to render the block and its name. Never stock, vault or trusted list. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.put("Offers", encodeOffers(registries));
		tag.putString("OwnerName", ownerName);
		if (shopName != null) {
			tag.putString("ShopName", shopName);
		}
		return tag;
	}

	@Nullable
	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	// ---------------------------------------------------------------- client rendering

	public ItemStack getItemToRender() {
		if (offers.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (tradeIndex >= offers.size()) {
			tradeIndex = 0;
		}
		return offers.get(tradeIndex).template();
	}

	public static void clientTick(Level level, BlockPos pos, BlockState state, ShopBlockEntity shop) {
		if (level.getGameTime() % 60 == 0) {
			shop.tradeIndex++;
		}
	}
}
