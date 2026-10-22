package dev.gaaus.cobblemonplayershops.shop;

import dev.gaaus.cobblemonplayershops.CobblemonPlayerShops;
import dev.gaaus.cobblemonplayershops.menu.ShopMenus;
import dev.gaaus.cobblemonplayershops.permission.Perms;
import dev.gaaus.cobblemonplayershops.registry.ModBlockEntities;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Shared behaviour of the player shop and the admin shop. */
public abstract class ShopBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The model's elements in model space (open shelf side = south). */
	private static final double[][] BOXES = {
		{0, 14, 0, 16, 16, 16},  // slab top
		{0, 1, 5, 1, 14, 16},    // west wall
		{15, 1, 5, 16, 14, 16},  // east wall
		{0, 0, 4, 16, 14, 5},    // back panel
		{0, 0, 5, 16, 1, 16},    // floor
		{1, 10, 5, 15, 11, 16},  // upper shelf
		{1, 6, 5, 15, 7, 16}};   // lower shelf
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			SHAPES.put(facing, shapeFacing(facing));
		}
	}

	/** Rotates the boxes the same way the blockstate's "y" rotation turns the model. */
	private static VoxelShape shapeFacing(Direction facing) {
		VoxelShape shape = Shapes.empty();
		for (double[] b : BOXES) {
			double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
			double[] r = switch (facing) {
				case WEST -> new double[] {16 - z2, x1, 16 - z1, x2};
				case NORTH -> new double[] {16 - x2, 16 - z2, 16 - x1, 16 - z1};
				case EAST -> new double[] {z1, 16 - x2, z2, 16 - x1};
				default -> new double[] {x1, z1, x2, z2};
			};
			shape = Shapes.or(shape, Block.box(r[0], b[1], r[1], r[2], b[4], r[3]));
		}
		return shape.optimize();
	}

	protected ShopBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
	}

	public abstract boolean isAdminShop();

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Nullable
	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
		// Spectators would otherwise open the block entity's MenuProvider.
		return null;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ShopBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, ModBlockEntities.SHOP, level.isClientSide ? ShopBlockEntity::clientTick : ShopBlockEntity::serverTick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (!ShopProtection.isRealPlayer(player) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.FAIL;
		}
		if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity shop)) {
			return InteractionResult.FAIL;
		}
		shop.refreshTrustedName(serverPlayer);
		ShopMenus.openMerchantMenu(serverPlayer, shop);
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide) {
			return;
		}
		if (!(placer instanceof ServerPlayer serverPlayer) || !ShopProtection.isRealPlayer(serverPlayer)) {
			// Dispensers, fake players, command blocks, ...: no owner could be established, so refuse.
			level.removeBlock(pos, false);
			Block.popResource(level, pos, stack.copyWithCount(1));
			return;
		}
		if (level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
			shop.initializeForPlacement(serverPlayer, Perms.isAdmin(serverPlayer));
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		// Only recorded for the log line. Payout logic lives in onRemove and never looks at the breaker.
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
			shop.noteBreaker(player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** The single removal path, whatever caused it: player, explosion, /setblock, WorldEdit, ... */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
			try {
				shop.closeAllViewers();
				if (!shop.isAdminShop()) {
					Containers.dropContents(level, pos, shop);
					shop.clearContent();
				}
				ShopTransactions.payoutVault(shop, "removed", shop.getLastBreaker());
			} catch (RuntimeException e) {
				CobblemonPlayerShops.LOGGER.error("Error while removing shop at {} (vault {})", pos, shop.getVault(), e);
			}
		}
		super.onRemove(state, level, pos, newState, moved);
	}
}
