package com.caszgamermd.caszualcaszual_tv_time.block;

import com.caszgamermd.caszualcaszual_tv_time.item.PortableTvSettings;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PortableTvBlock extends BaseEntityBlock {
    public static final MapCodec<PortableTvBlock> CODEC =
        simpleCodec(PortableTvBlock::new);

    private static final VoxelShape NORTH =
        box(2, 1, 8, 14, 12, 16);
    private static final VoxelShape SOUTH =
        box(2, 1, 0, 14, 12, 8);
    private static final VoxelShape EAST =
        box(0, 1, 2, 8, 12, 14);
    private static final VoxelShape WEST =
        box(8, 1, 2, 16, 12, 14);

    public PortableTvBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(TvBlock.FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
            TvBlock.FACING,
            context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return switch (state.getValue(TvBlock.FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
            var settings = PortableTvSettings.read(stack);
            tv.setChannel(settings.channel());
            tv.setDisplayMode(settings.displayMode());
            tv.setTvAudioEnabled(settings.tvAudioEnabled());
        }
    }

    @Override
    protected BlockState rotate(
        BlockState state,
        Rotation rotation
    ) {
        return state.setValue(
            TvBlock.FACING,
            rotation.rotate(state.getValue(TvBlock.FACING))
        );
    }

    @Override
    protected BlockState mirror(
        BlockState state,
        Mirror mirror
    ) {
        return state.rotate(
            mirror.getRotation(state.getValue(TvBlock.FACING))
        );
    }

    @Override
    protected void createBlockStateDefinition(
        StateDefinition.Builder<
            net.minecraft.world.level.block.Block,
            BlockState
        > builder
    ) {
        builder.add(TvBlock.FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
        BlockPos pos,
        BlockState state
    ) {
        return new TvBlockEntity(pos, state);
    }
}
