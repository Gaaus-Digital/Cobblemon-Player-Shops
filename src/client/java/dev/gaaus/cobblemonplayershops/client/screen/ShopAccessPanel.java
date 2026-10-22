package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.menu.ShopMenuData;
import dev.gaaus.cobblemonplayershops.network.c2s.AddTrustedC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.RemoveTrustedC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.SetTrustedPermsC2S;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

/**
 * Client-only access settings panel drawn inside the settings overlay of the merchant screen (no extra server menu:
 * packets keep using the merchant menu's container id). Everything it shows is display-only; the server re-checks every packet.
 */
final class ShopAccessPanel {
	interface Host {
		<T extends GuiEventListener & Renderable & NarratableEntry> T addPanelWidget(T widget);

		void removePanelWidget(GuiEventListener widget);

		Font font();

		int originX();

		int originY();

		int containerId();

		int guiWidth();

		int guiHeight();

		ShopMenuData data();
	}

	/** Panel position inside the screen; everything below is relative to the panel's own top-left corner. */
	private static final int X = 6;
	private static final int Y = 20;
	private static final int NAME_Y = 20;
	private static final int HEADER_Y = 44;
	private static final int SEPARATOR_Y = 55;
	private static final int ROWS_Y = 60;
	private static final int ROW_H = 20;
	private static final int INVENTORY_CENTER_X = 128;
	private static final int WITHDRAW_CENTER_X = 186;

	private final Host host;
	private boolean open;
	private int scroll;
	private EditBox nameBox;
	private CdButton addButton;
	private final List<AbstractWidget> rowWidgets = new ArrayList<>();

	ShopAccessPanel(Host host) {
		this.host = host;
	}

	private int width() {
		return host.guiWidth() - 2 * X;
	}

	private int height() {
		return host.guiHeight() - Y - 4;
	}

	private int visibleRows() {
		return Math.max(1, (height() - ROWS_Y - 6) / ROW_H);
	}

	boolean isOpen() {
		return open;
	}

	void setOpen(boolean value) {
		open = value;
		refresh();
	}

	void init() {
		String previous = nameBox == null ? "" : nameBox.getValue();
		rowWidgets.clear();
		Font font = host.font();
		int px = host.originX() + X;
		int py = host.originY() + Y;

		nameBox = host.addPanelWidget(new EditBox(font, px + 10, py + NAME_Y + 4, 122, 9, Component.translatable("gui.cobblemon_player_shops.access.name_hint")));
		nameBox.setBordered(false);
		nameBox.setTextColor(0xFFFFFF);
		nameBox.setMaxLength(16);
		nameBox.setFilter(s -> s.matches("[A-Za-z0-9_]*"));
		nameBox.setHint(Component.translatable("gui.cobblemon_player_shops.access.name_hint"));
		nameBox.setValue(previous);

		addButton = host.addPanelWidget(new CdButton(px + 142, py + NAME_Y + 1, 50, Component.translatable("gui.cobblemon_player_shops.access.add"), false, this::submitName));
		refresh();
	}

	private void submitName() {
		String name = nameBox.getValue();
		if (!name.isEmpty()) {
			ClientPlayNetworking.send(new AddTrustedC2S(host.containerId(), name));
			nameBox.setValue("");
		}
	}

	/** Rebuilds the row widgets from the latest data and applies visibility. */
	void refresh() {
		if (nameBox == null) {
			return;
		}
		for (AbstractWidget old : rowWidgets) {
			host.removePanelWidget(old);
		}
		rowWidgets.clear();

		if (!host.data().canManageAccess()) {
			open = false;
		}
		nameBox.visible = open;
		nameBox.active = open;
		addButton.visible = open;
		addButton.active = open;
		if (!open) {
			nameBox.setFocused(false);
			return;
		}

		List<ShopMenuData.TrustedView> trusted = host.data().trusted();
		int rows = visibleRows();
		scroll = Math.max(0, Math.min(scroll, Math.max(0, trusted.size() - rows)));
		int px = host.originX() + X;
		int py = host.originY() + Y;
		Font font = host.font();
		int box = Checkbox.getBoxSize(font);

		for (int i = 0; i < rows && scroll + i < trusted.size(); i++) {
			ShopMenuData.TrustedView entry = trusted.get(scroll + i);
			int y = py + ROWS_Y + i * ROW_H + (ROW_H - box) / 2;
			Checkbox[] boxes = new Checkbox[2];
			// Each checkbox sends the live state of BOTH boxes in its row, so quick double clicks cannot undo each other.
			Checkbox.OnValueChange send = (changed, value) -> ClientPlayNetworking.send(
				new SetTrustedPermsC2S(host.containerId(), entry.id(), boxes[0].selected(), boxes[1].selected()));
			boxes[0] = Checkbox.builder(Component.empty(), font).pos(px + INVENTORY_CENTER_X - box / 2, y)
				.selected(entry.canAccessInventory()).onValueChange(send).build();
			boxes[1] = Checkbox.builder(Component.empty(), font).pos(px + WITHDRAW_CENTER_X - box / 2, y)
				.selected(entry.canWithdraw()).onValueChange(send).build();
			CdIconButton remove = new CdIconButton(px + width() - 8 - 12, y + box / 2 - CdTextures.BUTTON_HEIGHT / 2, 12, ShopGui.ICON_CROSS,
				Component.translatable("gui.cobblemon_player_shops.access.remove"), false,
				() -> ClientPlayNetworking.send(new RemoveTrustedC2S(host.containerId(), entry.id())));

			rowWidgets.add(host.addPanelWidget(boxes[0]));
			rowWidgets.add(host.addPanelWidget(boxes[1]));
			rowWidgets.add(host.addPanelWidget(remove));
		}
	}

	/** Panel body and texts; drawn behind the widgets. */
	void renderBackground(GuiGraphics g) {
		if (!open) {
			return;
		}
		Font font = host.font();
		int w = width();
		int px = host.originX() + X;
		int py = host.originY() + Y;
		ShopGui.inset(g, px, py, w, height());
		ShopGui.inset(g, px + 6, py + NAME_Y, 130, 16);
		ShopMenuData data = host.data();
		ShopGui.text(g, font, Component.translatable("gui.cobblemon_player_shops.access.title"), px + 6, py + 6, ShopGui.COLOR_TEXT);
		Component count = Component.translatable("gui.cobblemon_player_shops.access.count", data.trusted().size(), data.maxTrusted());
		ShopGui.text(g, font, count, px + w - 6 - font.width(count), py + 6, ShopGui.COLOR_TEXT);

		ShopGui.text(g, font, Component.translatable("gui.cobblemon_player_shops.access.player"), px + 6, py + HEADER_Y, ShopGui.COLOR_TEXT);
		drawCentered(g, font, Component.translatable("gui.cobblemon_player_shops.access.inventory"), px + INVENTORY_CENTER_X, py + HEADER_Y);
		drawCentered(g, font, Component.translatable("gui.cobblemon_player_shops.access.withdraw"), px + WITHDRAW_CENTER_X, py + HEADER_Y);
		g.fill(px + 6, py + SEPARATOR_Y, px + w - 6, py + SEPARATOR_Y + 1, ShopGui.COLOR_DARK);

		List<ShopMenuData.TrustedView> trusted = data.trusted();
		if (trusted.isEmpty()) {
			ShopGui.text(g, font, Component.translatable("gui.cobblemon_player_shops.access.empty"), px + 6, py + ROWS_Y + 6, ShopGui.COLOR_TEXT);
		}
		int nameSpace = INVENTORY_CENTER_X - Checkbox.getBoxSize(font) / 2 - 6 - 6;
		int rows = visibleRows();
		for (int i = 0; i < rows && scroll + i < trusted.size(); i++) {
			String name = font.plainSubstrByWidth(trusted.get(scroll + i).name(), nameSpace);
			ShopGui.text(g, font, name, px + 6, py + ROWS_Y + i * ROW_H + (ROW_H - font.lineHeight) / 2, ShopGui.COLOR_TEXT);
		}
	}

	private static void drawCentered(GuiGraphics g, Font font, Component text, int centerX, int y) {
		ShopGui.text(g, font, text, centerX - font.width(text) / 2, y, ShopGui.COLOR_TEXT);
	}

	boolean mouseScrolled(double mx, double my, double delta) {
		if (!open) {
			return false;
		}
		scroll -= (int) Math.signum(delta);
		refresh();
		return true;
	}

	boolean keyPressed(int keyCode) {
		// Enter in the name box adds the player.
		if (open && nameBox != null && nameBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
			submitName();
			return true;
		}
		return false;
	}
}
