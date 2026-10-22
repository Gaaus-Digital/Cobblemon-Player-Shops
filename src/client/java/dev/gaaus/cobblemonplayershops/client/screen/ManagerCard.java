package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.menu.ShopMenuData;
import dev.gaaus.cobblemonplayershops.network.c2s.OpenInventoryC2S;
import dev.gaaus.cobblemonplayershops.network.c2s.WithdrawC2S;
import dev.gaaus.cobblemonplayershops.shop.ShopRole;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

/**
 * The card that fills the merchant view's top-middle slot. Exactly one of three faces is shown: the manager card
 * (balance, withdraw, inventory, settings), the admin card, or the plain shop info card for everyone else.
 * Which buttons exist is decided by the permission flags the server sent; the server re-checks every packet.
 */
final class ManagerCard {
	enum Kind {
		MANAGER, ADMIN, INFO
	}

	static final int X = 92;
	private static final int TEXT_X = 96;
	private static final int BUTTON_X = 95;
	private static final int BUTTON_W = 72;
	private static final int BUTTON_H = 14;
	private static final int BUTTON_GAP = 4;
	private static final int FIRST_BUTTON_Y = 38;
	private static final int PILL_Y = 18;

	private final ShopMerchantScreen screen;
	private CdIconButton withdraw;
	private CdButton inventory;
	private CdButton settings;

	ManagerCard(ShopMerchantScreen screen) {
		this.screen = screen;
	}

	Kind kind() {
		ShopMenuData d = screen.data();
		if (d.admin() && d.canEditOffers()) {
			return Kind.ADMIN;
		}
		if (!d.admin() && (d.role() == ShopRole.OWNER || d.role() == ShopRole.TRUSTED)) {
			return Kind.MANAGER;
		}
		return Kind.INFO;
	}

	void build() {
		int left = screen.left();
		int top = screen.top();
		withdraw = screen.add(new CdIconButton(left + 151, top + PILL_Y, 14, ShopGui.ICON_WITHDRAW,
			Component.translatable("gui.cobblemon_player_shops.withdraw_tooltip"), true,
			() -> ClientPlayNetworking.send(new WithdrawC2S(screen.containerId()))));
		inventory = screen.add(new CdButton(left + BUTTON_X, top + FIRST_BUTTON_Y, BUTTON_W, Component.translatable("gui.cobblemon_player_shops.inventory"), true,
			() -> ClientPlayNetworking.send(new OpenInventoryC2S(screen.containerId()))));
		settings = screen.add(new CdButton(left + BUTTON_X, top + FIRST_BUTTON_Y, BUTTON_W, Component.translatable("gui.cobblemon_player_shops.settings"), true,
			screen::openSettings));
	}
	/** Re-evaluates which buttons exist and stacks the visible ones from the top without gaps. */
	void refresh() {
		if (withdraw == null) {
			return;
		}
		ShopMenuData d = screen.data();
		boolean manager = kind() == Kind.MANAGER && !screen.settingsOpen();
		withdraw.visible = manager && d.canWithdraw();
		withdraw.active = d.vault() > 0;
		int y = screen.top() + FIRST_BUTTON_Y;
		inventory.visible = manager && d.canAccessInventory();
		if (inventory.visible) {
			inventory.setY(y);
			y += BUTTON_H + BUTTON_GAP;
		}
		settings.visible = manager && d.canManageAccess();
		if (settings.visible) {
			settings.setY(y);
		}
	}

	void render(GuiGraphics g) {
		var font = screen.font();
		int left = screen.left();
		int top = screen.top();
		ShopMenuData d = screen.data();
		switch (kind()) {
			case MANAGER -> {
				ShopGui.text(g, font, Component.translatable("gui.cobblemon_player_shops.store_balance"), left + TEXT_X, top + 4, ShopGui.COLOR_TEXT);
				CdTextures.moneyPill(g, font, left + BUTTON_X, top + PILL_Y, MoneyFormat.compactPlain(d.vault()), ShopGui.COLOR_TEXT);
			}
			case ADMIN -> {
				ShopGui.text(g, font, Component.translatable("block.cobblemon_player_shops.admin_shop"), left + TEXT_X, top + 4, ShopGui.COLOR_TEXT);
				drawWrapped(g, Component.translatable("gui.cobblemon_player_shops.admin_card.unlimited"), left + TEXT_X, top + 20, 72);
				drawWrapped(g, Component.translatable("gui.cobblemon_player_shops.admin_card.sink"), left + TEXT_X, top + 40, 72);
			}
			case INFO -> {
				ShopGui.text(g, font, trimmedTitle(), left + TEXT_X, top + 4, ShopGui.COLOR_TEXT);
				Component owner = d.admin() || d.ownerName().isEmpty()
					? Component.translatable("gui.cobblemon_player_shops.info.admin_shop")
					: Component.translatable("gui.cobblemon_player_shops.info.owner", d.ownerName());
				drawWrapped(g, owner, left + TEXT_X, top + 20, 72);
				ShopGui.text(g, font, Component.translatable("gui.cobblemon_player_shops.info.offers", d.offers().size()),
					left + TEXT_X, top + 60, ShopGui.COLOR_TEXT);
			}
		}
	}

	private String trimmedTitle() {
		return screen.font().plainSubstrByWidth(screen.getTitle().getString(), 72);
	}

	private void drawWrapped(GuiGraphics g, Component text, int x, int y, int width) {
		var font = screen.font();
		List<FormattedCharSequence> lines = font.split(text, width);
		for (int i = 0; i < lines.size(); i++) {
			ShopGui.text(g, font, lines.get(i), x, y + i * font.lineHeight, ShopGui.COLOR_TEXT);
		}
	}
	@Nullable
	List<Component> tooltipAt(int mouseX, int mouseY) {
		int left = screen.left();
		int top = screen.top();
		if (kind() == Kind.MANAGER && ShopGui.inside(mouseX, mouseY, left + BUTTON_X, top + PILL_Y, 54, 14)) {
			return List.of(MoneyFormat.format(screen.data().vault()));
		}
		if (kind() == Kind.INFO && ShopGui.inside(mouseX, mouseY, left + TEXT_X, top + 2, 72, 12)
			&& !trimmedTitle().equals(screen.getTitle().getString())) {
			return List.of(screen.getTitle());
		}
		return null;
	}
}