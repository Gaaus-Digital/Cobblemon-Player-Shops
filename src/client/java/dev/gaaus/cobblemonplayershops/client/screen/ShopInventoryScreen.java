package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.economy.MoneyFormat;
import dev.gaaus.cobblemonplayershops.menu.ShopOwnerMenu;
import dev.gaaus.cobblemonplayershops.network.c2s.OpenMerchantC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The shop's stock (9x4) above the player inventory, on CobbleDollars' bank background. The server gates every move. */
public class ShopInventoryScreen extends AbstractContainerScreen<ShopOwnerMenu> {
	private static final int PILL_X = 58;
	private static final int PILL_Y = 189;

	public ShopInventoryScreen(ShopOwnerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = CdTextures.BANK_BASE.width();
		this.imageHeight = CdTextures.BANK_BASE.height();
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new CdButton(leftPos + 16, topPos + 189, 31, Component.translatable("gui.cobblemon_player_shops.back"), true,
			() -> ClientPlayNetworking.send(new OpenMerchantC2S(menu.containerId))));
	}

	/** Called after each refresh packet; the screen has no state of its own to update. */
	public void onDataRefreshed() {
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		CdTextures.blit(g, CdTextures.BANK_BASE, leftPos, topPos);
		Component header = Component.translatable("gui.cobblemon_player_shops.inventory");
		ShopGui.text(g, font, header, leftPos + 85 - font.width(header) / 2, topPos + 3, ShopGui.COLOR_TEXT);
		CdTextures.moneyPill(g, font, leftPos + PILL_X, topPos + PILL_Y, MoneyFormat.compactPlain(ShopGui.balance(minecraft)), ShopGui.COLOR_TEXT);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		// The header is drawn in renderBg.
	}

	@Override
	protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
		super.renderTooltip(g, mouseX, mouseY);
		if (ShopGui.inside(mouseX, mouseY, leftPos + PILL_X, topPos + PILL_Y, 54, 14)) {
			g.renderTooltip(font, MoneyFormat.format(ShopGui.balance(minecraft)), mouseX, mouseY);
		}
	}
}
