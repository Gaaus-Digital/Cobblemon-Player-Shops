package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.menu.ShopBuyMenu;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData.OfferView;
import dev.gaaus.cobblemonplayershops.network.c2s.DeleteOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SelectOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SetTemplateC2S;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The merchant view every player gets: an offer column with search, a purchase box, a card (manager / admin /
 * shop info), a display-only copy of the player's inventory, and for editors an inline offer editor. The menu has
 * no slots; everything here is cosmetic and the server re-checks every packet against the live shop.
 */
public class ShopMerchantScreen extends AbstractContainerScreen<ShopBuyMenu> {
	private static final int WIDTH = 252;
	private static final int HEIGHT = 196;
	private static final int INV_X = 5;
	private static final int INV_Y = 98;
	private static final int HOTBAR_Y = 156;
	private static final int BALANCE_X = 58;
	private static final int BALANCE_Y = 181;

	private OfferListWidget list;
	private final PurchaseBox purchase = new PurchaseBox(this);
	private final ManagerCard card = new ManagerCard(this);
	private final SettingsOverlay settings = new SettingsOverlay(this);

	private EditBox searchBox;
	private CdIconButton pencil;

	private String searchText = "";
	private int selectedOfferId = -1;
	private boolean editMode;
	private int editingOfferId = -1;

	public ShopMerchantScreen(ShopBuyMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = WIDTH;
		this.imageHeight = HEIGHT;
	}

	// ---------------------------------------------------------------- accessors for the helper classes

	ShopMenuData data() {
		return menu.data();
	}

	Font font() {
		return font;
	}

	int left() {
		return leftPos;
	}

	int top() {
		return topPos;
	}

	int containerId() {
		return menu.containerId;
	}

	ItemStack template() {
		return menu.templateBuffer();
	}

	Minecraft minecraftClient() {
		return minecraft;
	}

	String searchText() {
		return searchText;
	}

	boolean editMode() {
		return editMode;
	}

	int editingOfferId() {
		return editingOfferId;
	}

	int selectedOfferId() {
		return selectedOfferId;
	}

	boolean settingsOpen() {
		return settings.isOpen();
	}

	<T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget) {
		return addRenderableWidget(widget);
	}

	void remove(GuiEventListener widget) {
		removeWidget(widget);
	}

	@Nullable
	OfferView selectedOffer() {
		for (OfferView offer : data().offers()) {
			if (offer.id() == selectedOfferId) {
				return offer;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- init / refresh

	@Override
	protected void init() {
		super.init();
		list = addRenderableWidget(new OfferListWidget(this));
		int searchWidth = 73;
		searchBox = addRenderableWidget(new EditBox(font, leftPos + 175, topPos + 5, searchWidth, 9, Component.translatable("gui.cobblemon_player_shops.search_offers")));
		searchBox.setBordered(false);
		searchBox.setTextColor(0xFFFFFF);
		searchBox.setMaxLength(32);
		searchBox.setHint(Component.translatable("gui.cobblemon_player_shops.search_offers"));
		searchBox.setValue(searchText);
		searchBox.setResponder(text -> {
			searchText = text;
			list.refilter();
		});

		pencil = addRenderableWidget(new CdIconButton(leftPos + 236, topPos + 1, 14, ShopGui.ICON_PENCIL,
			Component.translatable("gui.cobblemon_player_shops.edit_offers"), true, () -> setEditMode(!editMode)));

		purchase.build();
		card.build();
		settings.build();
		onDataRefreshed();
	}

	/** Called after init and whenever a refresh packet arrived. */
	public void onDataRefreshed() {
		if (searchBox == null) {
			return;
		}
		ShopMenuData d = data();
		if (selectedOffer() == null) {
			selectedOfferId = -1;
		}
		if (editingOfferId >= 0 && d.offers().stream().noneMatch(o -> o.id() == editingOfferId)) {
			editingOfferId = -1;
		}
		if (editMode && !d.canEditOffers()) {
			editMode = false;
			editingOfferId = -1;
		}
		purchase.setQuantity(purchase.quantity());
		list.refilter();
		updateWidgets();
	}

	/** Re-runs visibility and enabled state for every widget. */
	void updateWidgets() {
		if (searchBox == null) {
			return;
		}
		settings.refresh();
		boolean overlay = settings.isOpen();
		list.visible = !overlay;
		boolean canEdit = data().canEditOffers();
		pencil.visible = canEdit && !overlay;
		pencil.setLitUp(editMode);
		pencil.setTooltipText(Component.translatable(editMode ? "gui.cobblemon_player_shops.stop_editing" : "gui.cobblemon_player_shops.edit_offers"));
		searchBox.visible = !overlay;
		searchBox.active = !overlay;
		searchBox.setWidth(canEdit ? 59 : 73);
		if (overlay) {
			searchBox.setFocused(false);
		}
		card.refresh();
		purchase.refresh();
	}

	// ---------------------------------------------------------------- state changes

	void setEditMode(boolean on) {
		if (on == editMode || (on && !data().canEditOffers())) {
			return;
		}
		editMode = on;
		editingOfferId = -1;
		purchase.resetForm();
		ClientPlayNetworking.send(new SetTemplateC2S(menu.containerId, -1));
		if (on) {
			selectedOfferId = -1;
		}
		list.refilter();
		updateWidgets();
	}

	void openSettings() {
		if (data().canManageAccess()) {
			settings.open();
		}
	}

	void onOfferRow(OfferView offer) {
		setFocused(null);
		selectedOfferId = offer.id();
		if (editMode) {
			editingOfferId = offer.id();
			purchase.setQuantity(offer.unitsPerPurchase());
			purchase.setPrice(String.valueOf(offer.price()));
			ClientPlayNetworking.send(new SelectOfferC2S(menu.containerId, offer.id()));
		} else {
			purchase.setQuantity(1);
		}
		updateWidgets();
	}

	void onNewOfferRow() {
		setFocused(null);
		selectedOfferId = -1;
		editingOfferId = -1;
		purchase.resetForm();
		ClientPlayNetworking.send(new SetTemplateC2S(menu.containerId, -1));
		list.refilter();
		updateWidgets();
	}

	void onDeleteOffer(OfferView offer) {
		setFocused(null);
		ClientPlayNetworking.send(new DeleteOfferC2S(menu.containerId, offer.id()));
		if (offer.id() == selectedOfferId || offer.id() == editingOfferId) {
			onNewOfferRow();
		}
	}

	/** After Save the form returns to a blank new offer, so Save cannot be pressed twice for the same edit. */
	void afterSave() {
		selectedOfferId = -1;
		editingOfferId = -1;
		purchase.resetForm();
		ClientPlayNetworking.send(new SetTemplateC2S(menu.containerId, -1));
		updateWidgets();
	}

	// ---------------------------------------------------------------- input

	/** Player inventory index for a display slot position, or -1. Hotbar is 0-8, main rows are 9-35. */
	private int inventoryIndexAt(double mx, double my) {
		int relX = (int) mx - leftPos - INV_X;
		if (mx < leftPos + INV_X || relX >= 9 * 18) {
			return -1;
		}
		int col = relX / 18;
		int relY = (int) my - topPos;
		if (relY >= INV_Y && relY < INV_Y + 3 * 18) {
			return 9 + (relY - INV_Y) / 18 * 9 + col;
		}
		if (relY >= HOTBAR_Y && relY < HOTBAR_Y + 18) {
			return col;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (!settings.isOpen()) {
			if (purchase.mouseClicked(mx, my, button)) {
				setFocused(null);
				return true;
			}
			if (editMode && button == 0) {
				int index = inventoryIndexAt(mx, my);
				if (index >= 0) {
					setFocused(null);
					ClientPlayNetworking.send(new SetTemplateC2S(menu.containerId, index));
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (getFocused() == list && isDragging() && button == 0 && list.mouseDragged(mx, my, button, dx, dy)) {
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		list.mouseReleased(mx, my, button);
		setDragging(false);
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (settings.isOpen()) {
			return settings.mouseScrolled(mx, my, scrollY);
		}
		if (purchase.mouseScrolled(mx, my, scrollY)) {
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (settings.keyPressed(keyCode)) {
			return true;
		}
		// While typing in a text box, letters/digits must not reach the screen (inventory key, hotbar swap).
		if (keyCode != 256 && getFocused() instanceof EditBox box && box.canConsumeInput()) {
			box.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	/** No slots exist, so a click outside the window must not produce a container click packet. */
	@Override
	protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
		return false;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		CdTextures.blit(g, CdTextures.SHOP_BASE, leftPos, topPos);
		if (settings.isOpen()) {
			settings.render(g);
			renderBalancePill(g);
			return;
		}

		renderStockTag(g);
		card.render(g);
		purchase.render(g, mouseX, mouseY);
		renderInventory(g, mouseX, mouseY);
		renderBalancePill(g);
	}

	private void renderBalancePill(GuiGraphics g) {
		CdTextures.moneyPill(g, font, leftPos + BALANCE_X, topPos + BALANCE_Y, MoneyFormat.compactPlain(ShopGui.balance(minecraft)), ShopGui.COLOR_TEXT);
	}

	private boolean overBalancePill(int mouseX, int mouseY) {
		return ShopGui.inside(mouseX, mouseY, leftPos + BALANCE_X, topPos + BALANCE_Y, 54, 14);
	}

	/** The stock tag poking out of the top-left: units the selected offer (or the edited item) can still serve. */
	private void renderStockTag(GuiGraphics g) {
		OfferView offer = editMode ? offerMatchingTemplate() : selectedOffer();
		if (offer == null) {
			return;
		}
		CdTextures.blit(g, CdTextures.STOCK, leftPos - 7, topPos + 8);
		Component label = Component.translatable("gui.cobblemon_player_shops.stock");
		ShopGui.text(g, font, label, leftPos + 20 - font.width(label) / 2, topPos + 11, ShopGui.COLOR_TEXT);
		boolean soldOut = offer.available() >= 0 && offer.availablePurchases() < 1;
		String amount = offer.available() < 0
			? Component.translatable("gui.cobblemon_player_shops.unlimited_symbol").getString()
			: String.valueOf(offer.available());
		ShopGui.text(g, font, amount, leftPos + 20 - font.width(amount) / 2, topPos + 24, soldOut ? ShopGui.COLOR_BAD : ShopGui.COLOR_TEXT);
	}
	@Nullable
	private OfferView offerMatchingTemplate() {
		ItemStack template = template();
		if (template.isEmpty()) {
			return null;
		}
		for (OfferView offer : data().offers()) {
			if (ItemStack.isSameItemSameComponents(offer.template(), template)) {
				return offer;
			}
		}
		return null;
	}

	private void renderInventory(GuiGraphics g, int mouseX, int mouseY) {
		var player = minecraft.player;
		int hovered = inventoryIndexAt(mouseX, mouseY);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				renderInventorySlot(g, player == null ? null : player.getInventory().getItem(9 + row * 9 + col),
					leftPos + INV_X + col * 18, topPos + INV_Y + row * 18, hovered == 9 + row * 9 + col);
			}
		}
		for (int col = 0; col < 9; col++) {
			renderInventorySlot(g, player == null ? null : player.getInventory().getItem(col),
				leftPos + INV_X + col * 18, topPos + HOTBAR_Y, hovered == col);
		}
	}

	private void renderInventorySlot(GuiGraphics g, @Nullable ItemStack stack, int x, int y, boolean hovered) {
		if (stack != null && !stack.isEmpty()) {
			g.renderItem(stack, x, y);
			g.renderItemDecorations(font, stack, x, y);
		}
		if (hovered) {
			if (editMode) {
				ShopGui.outline(g, x - 1, y - 1, 18, 18, 0xFFFFFFFF);
			}
			ShopGui.overlay(g, x, y, 16, 16, 0x20FFFFFF);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		// Nothing: the regions carry their own text.
	}

	@Override
	protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
		super.renderTooltip(g, mouseX, mouseY);
		if (settings.isOpen()) {
			if (overBalancePill(mouseX, mouseY)) {
				g.renderTooltip(font, MoneyFormat.format(ShopGui.balance(minecraft)), mouseX, mouseY);
			}
			return;
		}
		List<Component> lines = list.tooltipAt(mouseX, mouseY);
		if (lines == null) {
			lines = purchase.tooltipAt(mouseX, mouseY);
		}
		if (lines == null) {
			lines = card.tooltipAt(mouseX, mouseY);
		}
		if (lines == null && overBalancePill(mouseX, mouseY)) {
			lines = List.of(MoneyFormat.format(ShopGui.balance(minecraft)));
		}
		if (lines != null) {
			g.renderComponentTooltip(font, lines, mouseX, mouseY);
			return;
		}
		int index = inventoryIndexAt(mouseX, mouseY);
		if (index >= 0 && minecraft.player != null) {
			ItemStack stack = minecraft.player.getInventory().getItem(index);
			if (!stack.isEmpty()) {
				g.renderTooltip(font, stack, mouseX, mouseY);
			}
		}
	}
}
