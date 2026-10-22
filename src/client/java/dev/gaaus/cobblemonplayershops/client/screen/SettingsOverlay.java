package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.menu.ShopMenuData;
import dev.gaaus.cobblemonplayershops.network.c2s.ToggleHopperC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

/**
 * Owner-only settings drawn over the merchant view: the hopper toggle and the trusted-player access list
 * (which is {@link ShopAccessPanel}, hosted here). Display-only; the server re-checks every packet.
 */
final class SettingsOverlay implements ShopAccessPanel.Host {
	static final int WIDTH = 252;
	/** Leaves the bottom balance tab visible. */
	static final int HEIGHT = 183;
	private static final int PANEL_OFFSET_Y = 24;

	private final ShopMerchantScreen screen;
	private ShopAccessPanel panel;
	private CdButton back;
	private CdButton hopper;

	SettingsOverlay(ShopMerchantScreen screen) {
		this.screen = screen;
	}

	boolean isOpen() {
		return panel != null && panel.isOpen();
	}

	void build() {
		boolean wasOpen = isOpen();
		int left = screen.left();
		int top = screen.top();
		back = screen.add(new CdButton(left + WIDTH - 8 - 46, top + 5, 46, Component.translatable("gui.cobblemon_player_shops.back"), true, this::close));
		hopper = screen.add(new CdButton(left + 8, top + 23, 120, Component.empty(), false,
			() -> ClientPlayNetworking.send(new ToggleHopperC2S(screen.containerId()))));
		hopper.setTooltip(Tooltip.create(Component.translatable("gui.cobblemon_player_shops.hopper_tooltip")));
		panel = new ShopAccessPanel(this);
		panel.init();
		if (wasOpen) {
			panel.setOpen(true);
		}
	}

	void open() {
		panel.setOpen(true);
		screen.updateWidgets();
	}

	void close() {
		panel.setOpen(false);
		screen.updateWidgets();
	}

	/** Re-applies visibility; also closes itself when the viewer is no longer the owner. */
	void refresh() {
		if (panel == null) {
			return;
		}
		panel.refresh();
		ShopMenuData d = screen.data();
		boolean open = panel.isOpen();
		back.visible = open;
		hopper.visible = open && d.canEditOffers() && !d.admin();
		hopper.setMessage(Component.translatable(d.hopperInput() ? "gui.cobblemon_player_shops.hopper_on" : "gui.cobblemon_player_shops.hopper_off"));
	}

	void render(GuiGraphics g) {
		if (!isOpen()) {
			return;
		}
		ShopGui.panel(g, screen.left(), screen.top(), WIDTH, HEIGHT);
		ShopGui.text(g, screen.font(), Component.translatable("gui.cobblemon_player_shops.settings"), screen.left() + 8, screen.top() + 8, ShopGui.COLOR_TEXT);
		panel.renderBackground(g);
	}

	boolean mouseScrolled(double mx, double my, double delta) {
		return panel != null && panel.mouseScrolled(mx, my, delta);
	}

	boolean keyPressed(int keyCode) {
		return panel != null && panel.keyPressed(keyCode);
	}

	// ---------------------------------------------------------------- ShopAccessPanel.Host

	@Override
	public <T extends GuiEventListener & Renderable & NarratableEntry> T addPanelWidget(T widget) {
		return screen.add(widget);
	}

	@Override
	public void removePanelWidget(GuiEventListener widget) {
		screen.remove(widget);
	}

	@Override
	public Font font() {
		return screen.font();
	}

	@Override
	public int originX() {
		return screen.left();
	}

	@Override
	public int originY() {
		return screen.top() + PANEL_OFFSET_Y;
	}

	@Override
	public int containerId() {
		return screen.containerId();
	}

	@Override
	public int guiWidth() {
		return WIDTH;
	}

	/** The access panel fills the overlay below the hopper row, down to the overlay's lower border. */
	@Override
	public int guiHeight() {
		return HEIGHT - PANEL_OFFSET_Y;
	}

	@Override
	public ShopMenuData data() {
		return screen.data();
	}
}
