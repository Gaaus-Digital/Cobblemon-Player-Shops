package dev.gaaus.cobblemonplayershops.menu;

import dev.gaaus.cobblemonplayershops.network.s2c.ShopMenuRefreshS2C;
import dev.gaaus.cobblemonplayershops.registry.ModMenus;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Merchant menu, opened by everyone. Has NO slots at all, so no slot click can ever move an item; buying,
 * offer editing, withdrawing and settings are all packet-driven and validated by the server. The template
 * buffer used while editing an offer lives here and is only ever filled from server-side state.
 */
public class ShopBuyMenu extends AbstractContainerMenu implements ShopMenu {
	@Nullable
	private final ShopBlockEntity shop;
	private ShopMenuData data;
	private ItemStack templateBuffer = ItemStack.EMPTY;

	/** Server constructor. */
	public ShopBuyMenu(int containerId, Inventory inventory, ShopBlockEntity shop) {
		super(ModMenus.BUY, containerId);
		this.shop = shop;
		this.data = ShopMenuData.create(shop, inventory.player, ItemStack.EMPTY);
	}

	/** Client constructor. */
	public ShopBuyMenu(int containerId, Inventory inventory, ShopMenuData data) {
		super(ModMenus.BUY, containerId);
		this.shop = null;
		this.data = data;
		this.templateBuffer = data.template();
	}

	@Override
	@Nullable
	public ShopBlockEntity shop() {
		return shop;
	}

	public ShopMenuData data() {
		return data;
	}

	public ItemStack templateBuffer() {
		return templateBuffer;
	}

	/** Client: apply a refresh packet. */
	public void applyData(ShopMenuData newData) {
		this.data = newData;
		this.templateBuffer = newData.template();
	}

	/** Server: the buffer is only ever filled from the server's own inventory or an existing offer. */
	public void setTemplateBuffer(ItemStack stack) {
		this.templateBuffer = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
	}

	/** Server: push fresh offers / stock counts to this viewer. */
	public void sendRefresh(ServerPlayer player) {
		if (shop == null) {
			return;
		}
		data = ShopMenuData.create(shop, player, templateBuffer);
		ServerPlayNetworking.send(player, new ShopMenuRefreshS2C(containerId, data));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		// No slots: nothing to click.
	}

	@Override
	public boolean stillValid(Player player) {
		return shop == null || shop.stillValid(player);
	}
}
