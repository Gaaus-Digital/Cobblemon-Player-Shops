package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData.OfferView;
import dev.gaaus.cobblemonplayershops.network.c2s.BuyOfferC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SaveOfferC2S;
import dev.gaaus.cobblemonplayershops.shop.InventoryUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The purchase box under the stock tag: item preview, quantity field with arrows, total pill and the Buy button.
 * In edit mode the same widgets become the offer editor (template preview, units per sale, price, Save).
 * All checks here are cosmetic; the server enforces everything.
 */
final class PurchaseBox {
	private static final int WELL_X = 4;
	private static final int WELL_Y = 40;
	private static final int QTY_X = 36;
	private static final int QTY_Y = 62;
	private static final int ARROW_X = 64;
	private static final int PILL_X = 3;
	private static final int PILL_Y = 75;
	private static final int PILL_W = 52;
	private static final int PILL_H = 14;

	private final ShopMerchantScreen screen;
	private EditBox qtyBox;
	private EditBox priceBox;
	private CdButton action;

	/** Survive a re-init (window resize), which recreates the widgets. */
	private String qtyText = "1";
	private String priceText = "";
	private boolean adjusting;
	@Nullable
	private Component lastTip;

	PurchaseBox(ShopMerchantScreen screen) {
		this.screen = screen;
	}

	void build() {
		int left = screen.left();
		int top = screen.top();
		var font = screen.font();
		qtyBox = screen.add(new EditBox(font, left + 37, top + 64, 24, 9, Component.translatable("gui.cobblemon_player_shops.qty_hint")));
		qtyBox.setBordered(false);
		qtyBox.setTextColor(0xFFFFFF);
		qtyBox.setMaxLength(3);
		qtyBox.setFilter(s -> s.matches("[0-9]*"));
		qtyBox.setHint(Component.translatable("gui.cobblemon_player_shops.qty_hint"));
		qtyBox.setValue(qtyText);
		qtyBox.setResponder(this::onQtyTyped);

		priceBox = screen.add(new EditBox(font, left + 6, top + 78, 26, 9, Component.translatable("gui.cobblemon_player_shops.price_hint")));
		priceBox.setBordered(false);
		priceBox.setTextColor(0xFFFFFF);
		priceBox.setMaxLength(12);
		priceBox.setFilter(s -> s.matches("[0-9]*"));
		priceBox.setHint(Component.translatable("gui.cobblemon_player_shops.price_hint"));
		priceBox.setValue(priceText);
		priceBox.setResponder(s -> {
			priceText = s;
			refresh();
		});

		action = screen.add(new CdButton(left + 58, top + PILL_Y, 31, Component.empty(), false, this::onAction));
		lastTip = null;
	}

	EditBox qtyBox() {
		return qtyBox;
	}

	EditBox priceBox() {
		return priceBox;
	}

	// ---------------------------------------------------------------- values

	private static int parse(String text) {
		try {
			return text.isEmpty() ? 1 : Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	/** Highest value the field accepts: units per sale in edit mode, the server's purchase cap otherwise. */
	private int fieldMax() {
		if (screen.editMode()) {
			ItemStack template = screen.template();
			return template.isEmpty() ? 64 : Math.max(1, template.getMaxStackSize());
		}
		return Math.max(1, screen.data().maxQuantity());
	}

	int quantity() {
		return Math.max(1, Math.min(fieldMax(), parse(qtyText)));
	}

	private void onQtyTyped(String text) {
		if (adjusting) {
			return;
		}
		qtyText = text;
		if (!text.isEmpty()) {
			int value = parse(text);
			int max = fieldMax();
			if (value > max || value < 1) {
				setQuantity(Math.max(1, Math.min(max, value)));
				return;
			}
		}
		refresh();
	}

	void setQuantity(int value) {
		int clamped = Math.max(1, Math.min(fieldMax(), value));
		adjusting = true;
		qtyText = String.valueOf(clamped);
		if (qtyBox != null) {
			qtyBox.setValue(qtyText);
		}
		adjusting = false;
		refresh();
	}

	void setPrice(String text) {
		priceText = text;
		if (priceBox != null) {
			priceBox.setValue(text);
		}
		refresh();
	}

	/** Highest quantity that is affordable, in stock and within the server cap (never less than 1). */
	private int maxAffordableQuantity(OfferView offer) {
		long cap = Math.min(screen.data().maxQuantity(), offer.availablePurchases());
		BigInteger affordable = ShopGui.balance(screen.minecraftClient()).divide(BigInteger.valueOf(Math.max(1, offer.price())));
		if (affordable.compareTo(BigInteger.valueOf(cap)) < 0) {
			cap = affordable.longValue();
		}
		return (int) Math.max(1, cap);
	}

	@Nullable
	private Component buyBlocker(@Nullable OfferView offer, int qty) {
		if (offer == null) {
			return Component.translatable("gui.cobblemon_player_shops.buy.tooltip.select");
		}
		if (offer.availablePurchases() < 1) {
			return Component.translatable("gui.cobblemon_player_shops.buy.tooltip.sold_out");
		}
		if (offer.availablePurchases() < qty) {
			return Component.translatable("gui.cobblemon_player_shops.buy.tooltip.not_enough_stock");
		}
		BigInteger total = BigInteger.valueOf(offer.price()).multiply(BigInteger.valueOf(qty));
		if (ShopGui.balance(screen.minecraftClient()).compareTo(total) < 0) {
			return Component.translatable("gui.cobblemon_player_shops.buy.tooltip.no_money");
		}
		var player = screen.minecraftClient().player;
		if (player != null && !InventoryUtil.canFit(player.getInventory(), offer.template(), (long) offer.unitsPerPurchase() * qty)) {
			return Component.translatable("gui.cobblemon_player_shops.buy.tooltip.no_space");
		}
		return null;
	}

	/** Parsed price or -1 when the field is not a usable price. */
	private long editPrice() {
		try {
			long price = Long.parseLong(priceText);
			return price >= 1 && price <= screen.data().maxPrice() ? price : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	@Nullable
	private Component saveBlocker() {
		if (screen.template().isEmpty()) {
			return Component.translatable("gui.cobblemon_player_shops.save_need_item");
		}
		if (editPrice() < 0) {
			return Component.translatable("gui.cobblemon_player_shops.save_need_price", MoneyFormat.format(screen.data().maxPrice()));
		}
		return null;
	}

	// ---------------------------------------------------------------- widget state

	/** Applies visibility, enabled state and the action tooltip. Safe to call at any time. */
	void refresh() {
		if (action == null) {
			return;
		}
		boolean edit = screen.editMode();
		boolean visible = !screen.settingsOpen();
		qtyBox.visible = visible;
		qtyBox.active = visible;
		priceBox.visible = visible && edit;
		priceBox.active = visible && edit;
		action.visible = visible;
		if (!visible) {
			qtyBox.setFocused(false);
			priceBox.setFocused(false);
			return;
		}
		if (!edit) {
			priceBox.setFocused(false);
		}
		action.setMessage(Component.translatable(edit ? "gui.cobblemon_player_shops.save" : "gui.cobblemon_player_shops.buy.buy"));

		Component tip;
		if (edit) {
			Component blocker = saveBlocker();
			action.active = blocker == null;
			tip = blocker != null ? blocker : Component.translatable("gui.cobblemon_player_shops.save_ok");
		} else {
			OfferView offer = screen.selectedOffer();
			int qty = quantity();
			Component blocker = buyBlocker(offer, qty);
			action.active = blocker == null;
			if (blocker != null) {
				tip = blocker;
			} else {
				long total = offer.price() * qty;
				tip = Component.translatable("gui.cobblemon_player_shops.buy.tooltip.ok", (long) offer.unitsPerPurchase() * qty,
					offer.template().getHoverName(), MoneyFormat.format(total));
			}
		}
		if (!tip.equals(lastTip)) {
			lastTip = tip;
			action.setTooltip(Tooltip.create(tip));
		}
	}

	private void onAction() {
		if (screen.editMode()) {
			save();
			return;
		}
		OfferView offer = screen.selectedOffer();
		int qty = quantity();
		if (offer != null && buyBlocker(offer, qty) == null) {
			ClientPlayNetworking.send(new BuyOfferC2S(screen.containerId(), offer.id(), offer.price(), qty));
		}
	}

	private void save() {
		long price = editPrice();
		ItemStack template = screen.template();
		int units = quantity();
		if (template.isEmpty() || price < 0 || units < 1 || units > template.getMaxStackSize()) {
			return;
		}
		ClientPlayNetworking.send(new SaveOfferC2S(screen.containerId(), screen.editingOfferId(), price, units));
		screen.afterSave();
	}

	/** Back to a blank "new offer" form. */
	void resetForm() {
		setQuantity(1);
		setPrice("");
	}

	// ---------------------------------------------------------------- input

	boolean mouseClicked(double mx, double my, int button) {
		if (button != 0) {
			return false;
		}
		int x = screen.left() + ARROW_X;
		int y = screen.top() + QTY_Y;
		boolean up = ShopGui.inside(mx, my, x, y, 5, 6);
		boolean down = ShopGui.inside(mx, my, x, y + 5, 5, 6);
		if (!up && !down) {
			return false;
		}
		int step = Screen.hasShiftDown() ? 10 : 1;
		if (Screen.hasControlDown()) {
			OfferView offer = screen.selectedOffer();
			int max = screen.editMode() || offer == null ? fieldMax() : maxAffordableQuantity(offer);
			setQuantity(up ? max : 1);
		} else {
			setQuantity(quantity() + (up ? step : -step));
		}
		return true;
	}

	boolean mouseScrolled(double mx, double my, double delta) {
		if (!ShopGui.inside(mx, my, screen.left() + QTY_X, screen.top() + QTY_Y, 36, 12)) {
			return false;
		}
		setQuantity(quantity() + (delta > 0 ? 1 : -1) * (Screen.hasShiftDown() ? 10 : 1));
		return true;
	}

	// ---------------------------------------------------------------- rendering

	void render(GuiGraphics g, int mouseX, int mouseY) {
		int left = screen.left();
		int top = screen.top();
		var font = screen.font();
		boolean edit = screen.editMode();

		ItemStack preview = previewStack();
		if (!preview.isEmpty()) {
			ShopGui.itemScaled(g, preview, left + WELL_X, top + WELL_Y, 2.0F);
			int count = edit ? 0 : preview.getCount();
			if (count > 1) {
				String text = Component.translatable("gui.cobblemon_player_shops.count_times", count).getString();
				g.pose().pushPose();
				g.pose().translate(0, 0, 300);
				ShopGui.text(g, font, text, left + WELL_X + 31 - font.width(text), top + WELL_Y + 23, ShopGui.COLOR_TEXT);
				g.pose().popPose();
			}
			drawLines(g, font.split(Component.literal(preview.getHoverName().getString()), 52), left + 38, top + 41, 2);
		} else if (edit) {
			ShopGui.icon(g, ShopGui.ICON_PLUS, left + WELL_X + 13, top + WELL_Y + 13, ShopGui.COLOR_LIGHT);
			drawLines(g, font.split(Component.translatable("gui.cobblemon_player_shops.pick_template_short"), 52), left + 38, top + 41, 2);
		}

		drawArrow(g, CdTextures.ARROW_UP, left + ARROW_X, top + QTY_Y, 5, mouseX, mouseY);
		drawArrow(g, CdTextures.ARROW_DOWN, left + ARROW_X, top + QTY_Y + 5, 6, mouseX, mouseY);

		if (!edit) {
			OfferView offer = screen.selectedOffer();
			if (offer != null) {
				BigInteger total = BigInteger.valueOf(offer.price()).multiply(BigInteger.valueOf(quantity()));
				boolean unaffordable = ShopGui.balance(screen.minecraftClient()).compareTo(total) < 0;
				String text = MoneyFormat.compactPlain(total);
				String shown = font.width(text) > CdTextures.PILL_TEXT_WIDTH ? font.plainSubstrByWidth(text, CdTextures.PILL_TEXT_WIDTH) : text;
				ShopGui.text(g, font, shown, left + 32 - font.width(shown), top + 78, unaffordable ? ShopGui.COLOR_BAD : ShopGui.COLOR_TEXT);
			}
		}
	}

	private void drawLines(GuiGraphics g, List<FormattedCharSequence> lines, int x, int y, int max) {
		var font = screen.font();
		for (int i = 0; i < Math.min(max, lines.size()); i++) {
			ShopGui.text(g, font, lines.get(i), x, y + i * font.lineHeight, ShopGui.COLOR_TEXT);
		}
	}

	/** The arrow textures hold a normal frame at v=0 and a hovered frame at v=5. */
	private void drawArrow(GuiGraphics g, CdTextures.Tex tex, int x, int y, int hitHeight, int mouseX, int mouseY) {
		boolean hover = ShopGui.inside(mouseX, mouseY, x, y, 5, hitHeight);
		CdTextures.blit(g, tex, x, y, 0, hover ? 5 : 0, 5, 5);
	}
	/** What the 2x preview shows: the selected offer in buy mode, the template buffer in edit mode. */
	private ItemStack previewStack() {
		if (screen.editMode()) {
			return screen.template();
		}
		OfferView offer = screen.selectedOffer();
		return offer == null ? ItemStack.EMPTY : offer.template();
	}

	@Nullable
	List<Component> tooltipAt(int mouseX, int mouseY) {
		int left = screen.left();
		int top = screen.top();
		if (ShopGui.inside(mouseX, mouseY, left + WELL_X, top + WELL_Y, 32, 32)) {
			ItemStack preview = previewStack();
			if (preview.isEmpty()) {
				return screen.editMode() ? List.of(Component.translatable("gui.cobblemon_player_shops.pick_template_hint")) : null;
			}
			List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(screen.minecraftClient(), preview));
			if (!screen.editMode()) {
				lines.add(Component.translatable("gui.cobblemon_player_shops.buy.per_purchase", preview.getCount()).withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("gui.cobblemon_player_shops.buy.units_total", (long) preview.getCount() * quantity()).withStyle(ChatFormatting.GRAY));
			}
			return lines;
		}
		if (ShopGui.inside(mouseX, mouseY, left + PILL_X, top + PILL_Y, PILL_W, PILL_H) && !screen.editMode()) {
			OfferView offer = screen.selectedOffer();
			if (offer != null) {
				return List.of(MoneyFormat.format(BigInteger.valueOf(offer.price()).multiply(BigInteger.valueOf(quantity()))));
			}
		}
		return null;
	}
}
