package dev.gaaus.cobblemonplayershops.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A button drawn with CobbleDollars' three-state button textures (buy style, or nav style for navigation). */
class CdButton extends AbstractButton {
	private final boolean nav;
	private final Runnable onPress;
	private boolean litUp;

	CdButton(int x, int y, int width, Component label, boolean nav, Runnable onPress) {
		super(x, y, width, CdTextures.BUTTON_HEIGHT, label);
		this.nav = nav;
		this.onPress = onPress;
	}

	@Override
	public void onPress() {
		onPress.run();
	}

	/** Shows the hovered look permanently, e.g. for a toggle that is switched on. */
	void setLitUp(boolean lit) {
		this.litUp = lit;
	}

	void setTooltipText(Component text) {
		setMessage(text);
		setTooltip(Tooltip.create(text));
	}

	int textColor() {
		return active ? 0xFFFFFFFF : 0xFFA0A0A0;
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		int state = !active ? 2 : isHoveredOrFocused() || litUp ? 1 : 0;
		CdTextures.button3Slice(g, nav ? CdTextures.BANK_BUTTON : CdTextures.BUY_BUTTON, getX(), getY(), getWidth(), state);
		renderLabel(g);
	}

	void renderLabel(GuiGraphics g) {
		var font = Minecraft.getInstance().font;
		Component label = getMessage();
		ShopGui.text(g, font, label, getX() + (getWidth() - font.width(label)) / 2, getY() + (getHeight() - font.lineHeight) / 2 + 1, textColor());
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
