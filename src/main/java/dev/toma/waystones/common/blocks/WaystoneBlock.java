package dev.toma.waystones.common.blocks;

import dev.toma.waystones.common.init.ModdedBlocks;
import dev.toma.waystones.common.world.WaystoneCapabilityProvider;
import dev.toma.waystones.network.NetworkManager;
import dev.toma.waystones.network.S2C_OpenWaystoneMenuPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class WaystoneBlock extends Block {

    public static final BooleanProperty LOWER = BooleanProperty.create("lower");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public WaystoneBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LOWER, false).setValue(ACTIVE, false));
    }

    public void activate(World world, BlockPos pos, PlayerEntity player) {
        world.destroyBlock(pos, false);
        world.destroyBlock(pos.above(), false);
        world.setBlock(pos, ModdedBlocks.WAYSTONE.defaultBlockState().setValue(LOWER, true).setValue(ACTIVE, true), 3);
        world.getCapability(WaystoneCapabilityProvider.CAPABILITY).ifPresent(provider -> provider.registerNewWaystone(pos, player));
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader reader, BlockPos pos) {
        return reader.isEmptyBlock(pos.above());
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult result) {
        if (!world.isClientSide) {
            BlockPos core = state.getValue(LOWER) ? pos : pos.below();
            BlockPos above = core.above(2);
            if (world.isEmptyBlock(above) && world.canSeeSky(above)) {
                world.getCapability(WaystoneCapabilityProvider.CAPABILITY).ifPresent(data -> NetworkManager.sendClientPacket((ServerPlayerEntity) player, new S2C_OpenWaystoneMenuPacket(core, data.serializeNBT())));
            }
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, BlockState oldState, boolean p_220082_5_) {
        boolean isActive = state.getValue(ACTIVE);
        if (state.getValue(LOWER)) {
            BlockState above = ModdedBlocks.WAYSTONE.defaultBlockState().setValue(LOWER, false).setValue(ACTIVE, isActive);
            world.setBlock(pos.above(), above, 3);
        }
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean p_196243_5_) {
        boolean isLower = state.getValue(LOWER);
        BlockPos pos1 = isLower ? pos.above() : pos.below();
        world.destroyBlock(pos1, false);
        if (isLower) {
            world.getCapability(WaystoneCapabilityProvider.CAPABILITY).ifPresent(provider -> provider.removeWaystone(pos));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(LOWER, ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return defaultBlockState().setValue(LOWER, true);
    }
}
