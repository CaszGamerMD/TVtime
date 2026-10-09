package com.caszgamermd.caszualtvtime.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SpeakerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING =
        HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<SpeakerSideStyle> SIDE_STYLE =
        EnumProperty.create("side_style", SpeakerSideStyle.class);

    public static final MapCodec<SpeakerBlock> CODEC =
        simpleCodec(SpeakerBlock::new);

    public SpeakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SIDE_STYLE, SpeakerSideStyle.IRON)
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
        Direction facing =
            context.getHorizontalDirection().getOpposite();

        if (ModBlocks.speakerStyle(this) == SpeakerStyle.MODERN
            && context.getClickedFace().getAxis().isHorizontal()) {
            facing = context.getClickedFace();
        }

        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return shapeFor(
            ModBlocks.speakerStyle(this),
            state.getValue(FACING)
        );
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

    private static VoxelShape shapeFor(
        SpeakerStyle style,
        Direction facing
    ) {
        if (style == SpeakerStyle.LEGACY) {
            return box(0, 0, 0, 16, 16, 16);
        }

        int width;
        int y0;
        int y1;
        int depth;

        switch (style) {
            case IRON -> {
                width = 14;
                y0 = 0;
                y1 = 16;
                depth = 12;
            }
            case SPRUCE -> {
                width = 12;
                y0 = 1;
                y1 = 15;
                depth = 10;
            }
            case MODERN -> {
                width = 10;
                y0 = 3;
                y1 = 13;
                depth = 3;
            }
            case CUSTOM -> {
                width = 12;
                y0 = 1;
                y1 = 14;
                depth = 8;
            }
            default -> {
                width = 16;
                y0 = 0;
                y1 = 16;
                depth = 16;
            }
        }

        double side0 = (16 - width) / 2.0;
        double side1 = 16 - side0;

        return switch (facing) {
            case NORTH -> box(
                side0, y0, 16 - depth,
                side1, y1, 16
            );
            case SOUTH -> box(
                side0, y0, 0,
                side1, y1, depth
            );
            case EAST -> box(
                0, y0, side0,
                depth, y1, side1
            );
            case WEST -> box(
                16 - depth, y0, side0,
                16, y1, side1
            );
            default -> box(
                side0, y0, 16 - depth,
                side1, y1, 16
            );
        };
    }

    @Override
    protected BlockState rotate(
        BlockState state,
        Rotation rotation
    ) {
        return state.setValue(
            FACING,
            rotation.rotate(state.getValue(FACING))
        );
    }

    @Override
    protected BlockState mirror(
        BlockState state,
        Mirror mirror
    ) {
        return state.rotate(
            mirror.getRotation(state.getValue(FACING))
        );
    }

    @Override
    protected void createBlockStateDefinition(
        StateDefinition.Builder<
            net.minecraft.world.level.block.Block,
            BlockState
        > builder
    ) {
        builder.add(FACING, SIDE_STYLE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
        BlockPos pos,
        BlockState state
    ) {
        return new SpeakerBlockEntity(pos, state);
    }
}
