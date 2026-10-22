package dev.gaaus.cobblemonplayershops.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A {@link CdButton} whose label is a small pixel icon; the message is only the tooltip and narration text. */
final class CdIconButton extends CdButton {
	private final String[] icon;

	CdIconButton(int x, int y, int width, String[] icon, Component tooltip, boolean nav, Runnable onPress) {
		super(x, y, width, tooltip, nav, onPress);
		this.icon = icon;
		setTooltip(Tooltip.create(tooltip));
	}

	@Override
	void renderLabel(GuiGraphics g) {
		int iconX = getX() + (getWidth() - ShopGui.iconWidth(icon)) / 2;
		int iconY = getY() + (getHeight() - icon.length) / 2;
		ShopGui.icon(g, icon, iconX, iconY, textColor());
	}
}
