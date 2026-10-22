package dev.gaaus.cobblemonplayershops.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * CobbleDollars' GUI textures, referenced by ResourceLocation at runtime only. CobbleDollars is "All rights
 * reserved", so none of its images are copied into or shipped with this mod; it is a required dependency, so
 * they are always loaded.
 */
final class CdTextures {
	record Tex(ResourceLocation id, int width, int height) {
	}

	private static Tex tex(String path, int width, int height) {
		return new Tex(ResourceLocation.fromNamespaceAndPath("cobbledollars", "textures/gui/" + path), width, height);
	}

	static final Tex SHOP_BASE = tex("shop/shop_base.png", 252, 196);
	static final Tex BANK_BASE = tex("bank/bank_base.png", 170, 204);
	static final Tex MONEY_PILL = tex("cobbledollars_background.png", 54, 14);
	static final Tex BUY_BUTTON = tex("shop/buy_button.png", 31, 42);
	static final Tex BANK_BUTTON = tex("shop/bank_button.png", 32, 42);
	static final Tex STOCK = tex("shop/stock.png", 54, 27);
	static final Tex OFFER_BACKGROUND = tex("shop/offer_background.png", 73, 16);
	static final Tex OFFER_OUTLINE = tex("shop/offer_outline.png", 75, 18);
	static final Tex ARROW_UP = tex("shop/amount_arrow_up.png", 5, 10);
	static final Tex ARROW_DOWN = tex("shop/amount_arrow_down.png", 5, 10);
	static final Tex CROSS = tex("cross.png", 5, 5);

	static final int BUTTON_HEIGHT = 14;
	/** Width of the pill's text area; the currency icon and edge follow it. */
	static final int PILL_TEXT_WIDTH = 29;

	private CdTextures() {
	}

	/** The whole texture at (x, y). */
	static void blit(GuiGraphics g, Tex tex, int x, int y) {
		g.blit(tex.id(), x, y, 0, 0, tex.width(), tex.height(), tex.width(), tex.height());
	}

	/** A region of the texture. */
	static void blit(GuiGraphics g, Tex tex, int x, int y, int u, int v, int w, int h) {
		g.blit(tex.id(), x, y, u, v, w, h, tex.width(), tex.height());
	}

	/** The money pill with its currency icon; the text is white (or red) with a shadow, ending at x + 29. */
	static void moneyPill(GuiGraphics g, Font font, int x, int y, String text, int color) {
		blit(g, MONEY_PILL, x, y);
		pillText(g, font, x, y, text, color);
	}

	private static void pillText(GuiGraphics g, Font font, int x, int y, String text, int color) {
		String shown = font.width(text) > PILL_TEXT_WIDTH ? font.plainSubstrByWidth(text, PILL_TEXT_WIDTH) : text;
		ShopGui.text(g, font, shown, x + PILL_TEXT_WIDTH - font.width(shown), y + 3, color);
	}

	/** Any width at the fixed button height, from a 3-state texture: state 0 normal, 1 hovered, 2 disabled. */
	static void button3Slice(GuiGraphics g, Tex tex, int x, int y, int w, int state) {
		int v = state * BUTTON_HEIGHT;
		blit(g, tex, x, y, 0, v, 3, BUTTON_HEIGHT);
		g.blit(tex.id(), x + 3, y, w - 6, BUTTON_HEIGHT, 3.0F, v, 1, BUTTON_HEIGHT, tex.width(), tex.height());
		blit(g, tex, x + w - 3, y, tex.width() - 3, v, 3, BUTTON_HEIGHT);
	}
}
