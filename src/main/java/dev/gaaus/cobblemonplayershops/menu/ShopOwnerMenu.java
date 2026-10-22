package dev.gaaus.cobblemonplayershops.menu;

import dev.gaaus.cobblemonplayershops.network.s2c.ShopMenuRefreshS2C;
import dev.gaaus.cobblemonplayershops.registry.ModMenus;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Inventory menu: the shop's stock slots plus the player inventory. Only opened for player shops and for
 * viewers with inventory permission; the permission is re-checked live on every slot operation.
 */
public class ShopOwnerMenu extends AbstractContainerMenu implements ShopMenu {
	public static final int STOCK_X = 5;
	public static final int STOCK_Y = 19;
	public static final int INV_X = 5;
	public static final int INV_Y = 106;
	public static final int HOTBAR_Y = 164;

	@Nullable
	private final ShopBlockEntity shop;
	private final Player viewer;
	private final int stockSlotCount;

	/** Last data known to this side. On the server it is rebuilt on demand, on the client it comes from packets. */
	private ShopMenuData data;

	/** Server constructor. */
	public ShopOwnerMenu(int containerId, Inventory inventory, ShopBlockEntity shop) {
		super(ModMenus.OWNER, containerId);
		this.shop = shop;
		this.viewer = inventory.player;
		this.data = ShopMenuData.create(shop, viewer, ItemStack.EMPTY);
		this.stockSlotCount = shop.isAdminShop() ? 0 : ShopBlockEntity.SIZE;
		addAllSlots(inventory, shop);
	}

	/** Client constructor (from the opening data). */
	public ShopOwnerMenu(int containerId, Inventory inventory, ShopMenuData data) {
		super(ModMenus.OWNER, containerId);
		this.shop = null;
		this.viewer = inventory.player;
		this.data = data;
		this.stockSlotCount = data.admin() ? 0 : ShopBlockEntity.SIZE;
		addAllSlots(inventory, new SimpleContainer(ShopBlockEntity.SIZE));
	}

	private void addAllSlots(Inventory inventory, Container stock) {
		for (int i = 0; i < stockSlotCount; i++) {
			addSlot(new StockSlot(stock, i, STOCK_X + (i % 9) * 18, STOCK_Y + (i / 9) * 18));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, 9 + row * 9 + col, INV_X + col * 18, INV_Y + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, INV_X + col * 18, HOTBAR_Y));
		}
	}

	@Override
	@Nullable
	public ShopBlockEntity shop() {
		return shop;
	}

	public ShopMenuData data() {
		return data;
	}

	public int stockSlotCount() {
		return stockSlotCount;
	}

	/** Client: apply a refresh packet. */
	public void applyData(ShopMenuData newData) {
		this.data = newData;
	}

	/** May this viewer put items into / take items out of the stock right now? Live on the server. */
	public boolean accessAllowed() {
		return shop != null ? shop.canAccessInventory(viewer) : data.canAccessInventory();
	}

	/** Server: push fresh data, or bounce a viewer who lost inventory permission back to the merchant view. */
	public void sendRefresh(ServerPlayer player) {
		if (shop == null) {
			return;
		}
		if (!shop.canAccessInventory(player)) {
			ShopMenus.openMerchantMenu(player, shop);
			return;
		}
		data = ShopMenuData.create(shop, player, ItemStack.EMPTY);
		ServerPlayNetworking.send(player, new ShopMenuRefreshS2C(containerId, data));
	}

	// ---------------------------------------------------------------- vanilla overrides

	@Override
	public boolean stillValid(Player player) {
		return shop == null || shop.stillValid(player);
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (shop != null) {
			// Live re-check on every slot operation: block gone / out of range / permission revoked.
			if (!shop.stillValid(player) || !shop.canAccessInventory(player)) {
				if (player instanceof ServerPlayer serverPlayer) {
					if (shop.stillValid(player)) {
						ShopMenus.openMerchantMenu(serverPlayer, shop);
					} else {
						serverPlayer.closeContainer();
					}
				}
				return;
			}
		}
		super.clicked(slotId, button, clickType, player);
	}

	/** Follows vanilla ChestMenu.quickMoveStack exactly, plus the live access check. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (stockSlotCount == 0 || !accessAllowed()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			result = stack.copy();
			if (index < stockSlotCount) {
				if (!this.moveItemStackTo(stack, stockSlotCount, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(stack, 0, stockSlotCount, false)) {
				return ItemStack.EMPTY;
			}
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return result;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		if (slot instanceof StockSlot && !accessAllowed()) {
			return false;
		}
		return super.canTakeItemForPickAll(stack, slot);
	}

	// ---------------------------------------------------------------- slots

	/** Stock slot gated by the viewer's live inventory-access permission (add AND remove). */
	public class StockSlot extends Slot {
		public StockSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return accessAllowed() && super.mayPlace(stack);
		}

		@Override
		public boolean mayPickup(Player player) {
			return accessAllowed();
		}
	}
}
