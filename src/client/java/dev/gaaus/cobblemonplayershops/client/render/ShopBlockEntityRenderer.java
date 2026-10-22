package dev.gaaus.cobblemonplayershops.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.gaaus.cobblemonplayershops.shop.ShopBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Floating, rotating item above the shop; cycles through the offers. */
public class ShopBlockEntityRenderer implements BlockEntityRenderer<ShopBlockEntity> {
	public ShopBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(ShopBlockEntity shop, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
		Level level = shop.getLevel();
		if (level == null) {
			return;
		}
		ItemStack toRender = shop.getItemToRender();
		if (toRender.isEmpty()) {
			return;
		}
		boolean isBlockItem = toRender.getItem() instanceof BlockItem;
		int lightAbove = LevelRenderer.getLightColor(level, shop.getBlockPos().above());

		poseStack.pushPose();
		double top = 1.0;
		poseStack.translate(0.5, top + (isBlockItem ? 0.10 : 0.20), 0.5);
		float scale = isBlockItem ? 0.95f : 0.85f;
		poseStack.scale(scale, scale, scale);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (System.currentTimeMillis() / 20d % 360d)));
		Minecraft.getInstance().getItemRenderer().renderStatic(toRender, ItemDisplayContext.GROUND, lightAbove, OverlayTexture.NO_OVERLAY,
			poseStack, buffer, level, 0);
		poseStack.popPose();
	}
}
