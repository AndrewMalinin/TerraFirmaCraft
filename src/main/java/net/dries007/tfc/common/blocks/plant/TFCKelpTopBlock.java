/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.plant;

import java.util.function.Supplier;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.IFluidLoggable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TFCKelpTopBlock extends TopPlantBlock implements IFluidLoggable
{
    public static TFCKelpTopBlock create(BlockBehaviour.Properties properties, Supplier<? extends Block> bodyBlock, Direction direction, VoxelShape shape, FluidProperty fluid)
    {
        return new TFCKelpTopBlock(properties, bodyBlock, direction, shape)
        {
            @Override
            public FluidProperty getFluidProperty()
            {
                return fluid;
            }
        };
    }

    private final Supplier<? extends Block> bodyBlock;

    protected TFCKelpTopBlock(BlockBehaviour.Properties properties, Supplier<? extends Block> bodyBlock, Direction direction, VoxelShape shape)
    {
        super(properties, bodyBlock, direction, shape);
        this.bodyBlock = bodyBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Level world = context.getLevel();
        BlockState state = defaultBlockState().setValue(AGE, world.getRandom().nextInt(25));
        FluidState fluidState = world.getFluidState(context.getClickedPos());
        if (getFluidProperty().canContain(fluidState.getType()))
        {
            return state.setValue(getFluidProperty(), getFluidProperty().keyFor(fluidState.getType()));
        }
        return null;
    }

    @Override
    protected boolean canAttachTo(BlockState state)
    {
        return state.getBlock() != Blocks.MAGMA_BLOCK;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    {
        VoxelShape voxelshape = super.getShape(state, worldIn, pos, context);
        Vec3 vector3d = state.getOffset(worldIn, pos);
        return voxelshape.move(vector3d.x, vector3d.y, vector3d.z);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos)
    {
        if (facing == growthDirection.getOpposite() && !stateIn.canSurvive(worldIn, currentPos))
        {
            worldIn.getBlockTicks().scheduleTick(currentPos, this, 1);
        }
        if (facing != growthDirection || !facingState.is(this) && !facingState.is(getBodyBlock()))
        {
            //Not sure if this is necessary
            Fluid fluid = stateIn.getFluidState().getType();
            worldIn.getLiquidTicks().scheduleTick(currentPos, fluid, fluid.getTickDelay(worldIn));
            return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
        }
        else// this is where it converts the top block to a body block when it gets placed on top of another top block
        {
            return this.getBodyBlock().defaultBlockState().setValue(getFluidProperty(), stateIn.getValue(getFluidProperty()));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(getFluidProperty());
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state)
    {
        return IFluidLoggable.super.getFluidState(state);
    }

    @Override
    public BlockBehaviour.OffsetType getOffsetType()
    {
        return BlockBehaviour.OffsetType.XZ;
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid)
    {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidStateIn)
    {
        return false;
    }

    @Override
    protected boolean canGrowInto(BlockState state)
    {
        Fluid fluid = state.getFluidState().getType();
        return getFluidProperty().canContain(fluid) && fluid != Fluids.EMPTY;
    }

    @Override
    protected GrowingPlantBodyBlock getBodyBlock()
    {
        return (GrowingPlantBodyBlock) bodyBlock.get();
    }
}
