package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData.OfferView;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * The offer column, a port of CobbleMerchant's offer list layout on vanilla {@link ObjectSelectionList}: the base
 * texture paints the list area, rows are inset by 2 px, and the scroller sits outside the right edge and only
 * exists while rows overflow. The screen decides what selecting, adding and deleting mean.
 */
final class OfferListWidget extends ObjectSelectionList<OfferListWidget.Entry> {
	private static final int ROW_WIDTH = 75;
	private static final int ROW_HEIGHT = 18;
	private static final int PANEL_COLOR = 0xFF676767;
	private static final int BEVEL_COLOR = 0xFF8D8D8D;
	private static final int BAD_PRICE_COLOR = 0xFFAA2020;

	private final ShopMerchantScreen screen;
	/** Set while the selection is rebuilt from the screen's state, so that no callback fires back into it. */
	private boolean restoring;

	OfferListWidget(ShopMerchantScreen screen) {
		super(screen.minecraftClient(), 79, 173, screen.top() + 16, ROW_HEIGHT);
		this.screen = screen;
		setX(screen.left() + 172);
	}

	/** Rebuilds the rows from the latest data and the current search text, keeping the selection and the scroll. */
	void refilter() {
		List<Entry> rows = new ArrayList<>();
		String query = screen.searchText().trim().toLowerCase(Locale.ROOT);
		for (OfferView offer : screen.data().offers()) {
			if (query.isEmpty() || offer.template().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
				rows.add(new Entry(offer));
			}
		}
		if (screen.editMode() && screen.data().offers().size() < screen.data().maxOffers()) {
			rows.add(new Entry(null));
		}
		replaceEntries(rows);

		restoring = true;
		for (Entry row : rows) {
			if (row.offer != null && row.offer.id() == screen.selectedOfferId()) {
				super.setSelected(row);
			}
		}
		restoring = false;
		setScrollAmount(getScrollAmount());
	}

	// ---------------------------------------------------------------- layout, as in CobbleMerchant

	@Override
	public int getRowWidth() {
		return ROW_WIDTH;
	}

	@Override
	public int getRowLeft() {
		return getX() + 2;
	}

	@Override
	public int getRowRight() {
		return getRowLeft() + getRowWidth();
	}

	@Override
	protected int getRowTop(int index) {
		return getY() + 2 - (int) getScrollAmount() + index * itemHeight;
	}

	@Override
	protected int getRowBottom(int index) {
		return getRowTop(index) + itemHeight;
	}

	@Override
	protected int getScrollbarPosition() {
		return getX() + width;
	}

	@Override
	protected void renderListBackground(GuiGraphics g) {
	}

	@Override
	protected void renderHeader(GuiGraphics g, int x, int y) {
	}

	@Override
	protected void renderListSeparators(GuiGraphics g) {
	}

	@Override
	protected void renderSelection(GuiGraphics g, int top, int width, int height, int borderColor, int fillColor) {
	}

	/** Widened to the outside scroller so that clicks, drags and wheel scrolling on it reach the list. */
	@Override
	public boolean isMouseOver(double mx, double my) {
		return visible && mx >= getX() && mx < getScrollbarPosition() + 7 && my >= getY() && my < getBottom();
	}

	@Override
	protected void renderDecorations(GuiGraphics g, int mouseX, int mouseY) {
		g.fillGradient(getX(), getY(), getRight(), getY() + 4, 0x80000000, 0x00000000);
		g.fillGradient(getX(), getBottom() - 4, getRight(), getBottom(), 0x00000000, 0x80000000);
		g.fill(getRight() - 1, getY(), getRight(), getY() + 1, PANEL_COLOR);
		g.fill(getRight() - 1, getBottom() - 1, getRight(), getBottom(), PANEL_COLOR);
		g.fill(getX(), getY(), getX() + 1, getY() + 1, PANEL_COLOR);
		g.fill(getX(), getBottom() - 1, getX() + 1, getBottom(), PANEL_COLOR);
		if (scrollbarVisible()) {
			int bar = getScrollbarPosition();
			g.hLine(bar, bar + 6, getY() - 1, BEVEL_COLOR);
			g.vLine(bar + 6, getY() - 1, getBottom(), BEVEL_COLOR);
			g.hLine(bar, bar + 6, getBottom(), BEVEL_COLOR);
		}
	}

	@Override
	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(g, mouseX, mouseY, partialTick);
		if (children().isEmpty()) {
			Component text = Component.translatable(screen.data().offers().isEmpty()
				? "gui.cobblemon_player_shops.no_offers" : "gui.cobblemon_player_shops.no_matches");
			drawWrapped(g, text, getX() + 3, getY() + 4, 73);
		}
	}

	private void drawWrapped(GuiGraphics g, Component text, int x, int y, int width) {
		var font = screen.font();
		List<FormattedCharSequence> lines = font.split(text, width);
		for (int i = 0; i < lines.size(); i++) {
			ShopGui.text(g, font, lines.get(i), x, y + i * font.lineHeight, ShopGui.COLOR_TEXT);
		}
		if (screen.editMode() && screen.data().offers().isEmpty()) {
			List<FormattedCharSequence> hintLines = font.split(Component.translatable("gui.cobblemon_player_shops.empty_edit_hint"), width);
			int base = y + ROW_HEIGHT + 4;
			for (int i = 0; i < hintLines.size(); i++) {
				ShopGui.text(g, font, hintLines.get(i), x, base + i * font.lineHeight, ShopGui.COLOR_TEXT);
			}
		}
	}

	// ---------------------------------------------------------------- selection

	/** The add row is never selectable; keyboard navigation selects offers exactly as a click does. */
	@Override
	public void setSelected(@Nullable Entry entry) {
		if (entry != null && entry.offer == null) {
			super.setSelected(null);
			return;
		}
		super.setSelected(entry);
		if (!restoring && entry != null && entry.offer.id() != screen.selectedOfferId()) {
			screen.onOfferRow(entry.offer);
		}
	}

	/** Tooltip lines for the row under the mouse, or null. */
	@Nullable
	List<Component> tooltipAt(int mouseX, int mouseY) {
		if (!visible || !isMouseOver(mouseX, mouseY)) {
			return null;
		}
		Entry entry = getHovered();
		if (entry == null) {
			return null;
		}
		OfferView offer = entry.offer;
		if (offer == null) {
			return List.of(Component.translatable("gui.cobblemon_player_shops.new_offer"));
		}
		if (screen.editMode() && entry == getSelected() && entry.isMouseOverCross(mouseX, mouseY)) {
			return List.of(Component.translatable("gui.cobblemon_player_shops.delete_offer"));
		}
		List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(screen.minecraftClient(), offer.template()));
		lines.add(Component.translatable("gui.cobblemon_player_shops.offer_price", MoneyFormat.format(offer.price())).withStyle(ChatFormatting.GOLD));
		if (offer.available() < 0) {
			lines.add(Component.translatable("gui.cobblemon_player_shops.offer_unlimited").withStyle(ChatFormatting.GRAY));
		} else if (offer.availablePurchases() < 1) {
			lines.add(Component.translatable("gui.cobblemon_player_shops.buy.sold_out").withStyle(ChatFormatting.RED));
		} else {
			lines.add(Component.translatable("gui.cobblemon_player_shops.offer_stock", offer.available()).withStyle(ChatFormatting.GRAY));
		}
		return lines;
	}

	/** One row. {@code offer == null} is the "+" row that opens a blank new-offer form. */
	final class Entry extends ObjectSelectionList.Entry<Entry> {
		@Nullable
		private final OfferView offer;
		private int lastLeft;
		private int lastTop;

		Entry(@Nullable OfferView offer) {
			this.offer = offer;
		}

		boolean isMouseOverCross(double mx, double my) {
			return mx >= lastLeft + 67 && mx < lastLeft + 72 && my >= lastTop + 3 && my < lastTop + 8;
		}

		@Override
		public void renderBack(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			CdTextures.blit(g, CdTextures.OFFER_BACKGROUND, left + 1, top + 1);
			if (hovering || isSelectedItem(index)) {
				g.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0x20FFFFFF);
				CdTextures.blit(g, CdTextures.OFFER_OUTLINE, left, top);
			}
		}

		@Override
		public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
			lastLeft = left;
			lastTop = top;
			var font = screen.font();
			if (offer == null) {
				g.drawCenteredString(font, "+", left + 37, top + 5, 0xFFFFFFFF);
				return;
			}
			g.renderItem(offer.template(), left + 1, top + 1);
			g.renderItemDecorations(font, offer.template(), left + 1, top + 1);
			boolean soldOut = offer.available() >= 0 && offer.availablePurchases() < 1;
			boolean unaffordable = ShopGui.balance(screen.minecraftClient()).compareTo(BigInteger.valueOf(offer.price())) < 0;
			CdTextures.moneyPill(g, font, left + 19, top + 2, MoneyFormat.compactPlain(offer.price()), soldOut || unaffordable ? BAD_PRICE_COLOR : 0xFFFFFFFF);
			if (screen.editMode() && this == getSelected()) {
				if (isMouseOverCross(mouseX, mouseY)) {
					g.setColor(0.82F, 0.24F, 0.24F, 1.0F);
				} else {
					g.setColor(0.4F, 0.4F, 0.4F, 1.0F);
				}
				CdTextures.blit(g, CdTextures.CROSS, left + 67, top + 3);
				g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			}
		}

		@Override
		public boolean mouseClicked(double mx, double my, int button) {
			if (button != 0) {
				return false;
			}
			if (offer == null) {
				screen.onNewOfferRow();
				return false;
			}
			if (screen.editMode() && this == getSelected() && isMouseOverCross(mx, my)) {
				screen.onDeleteOffer(offer);
				return false;
			}
			return true;
		}

		@Override
		public Component getNarration() {
			if (offer == null) {
				return Component.translatable("gui.cobblemon_player_shops.new_offer");
			}
			return Component.empty().append(offer.template().getHoverName()).append(" ").append(MoneyFormat.format(offer.price()));
		}
	}
}
