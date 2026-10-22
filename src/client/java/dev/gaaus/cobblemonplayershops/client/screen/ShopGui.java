package dev.gaaus.cobblemonplayershops.client.screen;

import dev.gaaus.cobblemonplayershops.economy.CobbleDollarsEconomy;
import java.math.BigInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Small drawing helpers in CobbleMerchant's palette. The merchant and inventory screens get their art from
 * {@link CdTextures}; the code-drawn panels here are only for the settings overlay and edit-mode outlines.
 */
final class ShopGui {
	static final int COLOR_PANEL = 0xFF676767;
	static final int COLOR_LIGHT = 0xFF8D8D8D;
	static final int COLOR_DARK = 0xFF2F2F2F;
	static final int COLOR_SLOT = 0xFF4B4B4B;
	static final int COLOR_SLOT_DARK = 0xFF2F2F2F;
	static final int COLOR_TEXT = 0xFFFFFFFF;
	static final int COLOR_BAD = 0xFFFF5555;

	static final String[] ICON_WITHDRAW = {
		"....#....",
		"....#....",
		"....#....",
		"..#####..",
		"...###...",
		"....#....",
		".........",
		"#.......#",
		"#########",
	};
	static final String[] ICON_PENCIL = {
		"......##.",
		".....####",
		"....####.",
		"...####..",
		"..####...",
		".####....",
		"####.....",
		"##.......",
	};
	static final String[] ICON_CROSS = {
		"#.....#",
		".#...#.",
		"..#.#..",
		"...#...",
		"..#.#..",
		".#...#.",
		"#.....#",
	};
	static final String[] ICON_PLUS = {
		"..#..",
		"..#..",
		"#####",
		"..#..",
		"..#..",
	};

	private ShopGui() {
	}

	/** Text on a dark surface: always drawn with a shadow. */
	static void text(GuiGraphics g, Font font, Component text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, true);
	}

	static void text(GuiGraphics g, Font font, String text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, true);
	}

	static void text(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
		g.drawString(font, text, x, y, color, true);
	}

	/** Outline, fill, a light top/left edge and a dark bottom/right edge. */
	static void panel(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, COLOR_DARK);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_PANEL);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, COLOR_LIGHT);
		g.fill(x + 1, y + 1, x + 2, y + h - 1, COLOR_LIGHT);
		g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, COLOR_SLOT);
		g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, COLOR_SLOT);
	}

	/** A dark field with a 1 px top/left shadow edge. */
	static void inset(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, COLOR_SLOT);
		g.fill(x, y, x + w, y + 1, COLOR_SLOT_DARK);
		g.fill(x, y, x + 1, y + h, COLOR_SLOT_DARK);
	}

	/** Draws an item at {@code scale}x. Decorations are not drawn (they would scale too). */
	static void itemScaled(GuiGraphics g, ItemStack stack, int x, int y, float scale) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(scale, scale, 1.0F);
		g.renderItem(stack, 0, 0);
		g.pose().popPose();
	}

	/** Tiny pixel art from '#' / '.' rows. */
	static void icon(GuiGraphics g, String[] pixels, int x, int y, int color) {
		for (int row = 0; row < pixels.length; row++) {
			String line = pixels[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
				}
			}
		}
	}

	static int iconWidth(String[] pixels) {
		return pixels[0].length();
	}

	/** A translucent fill that also covers items (items are drawn at a raised depth). */
	static void overlay(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.pose().pushPose();
		g.pose().translate(0, 0, 300);
		g.fill(x, y, x + w, y + h, color);
		g.pose().popPose();
	}

	static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	/** The player's own CobbleDollars for display; zero when unavailable. */
	static BigInteger balance(Minecraft minecraft) {
		try {
			return minecraft != null && minecraft.player != null ? CobbleDollarsEconomy.displayBalance(minecraft.player) : BigInteger.ZERO;
		} catch (RuntimeException e) {
			return BigInteger.ZERO;
		}
	}

	static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}
}
