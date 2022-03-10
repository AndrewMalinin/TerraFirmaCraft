/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;

public class WildDoubleCropBlock extends WildCropBlock
{
    public static final EnumProperty<DoubleCropBlock.Part> PART = TFCBlockStateProperties.DOUBLE_CROP_PART;

    public WildDoubleCropBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(PART, DoubleCropBlock.Part.BOTTOM));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return state.getValue(PART) == DoubleCropBlock.Part.BOTTOM ? CropBlock.FULL_SHAPE : CropBlock.HALF_SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(PART));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        final DoubleCropBlock.Part part = state.getValue(PART);
        final BlockState belowState = level.getBlockState(pos.below());
        if (part == DoubleCropBlock.Part.BOTTOM)
        {
            return TFCTags.Blocks.WILD_CROP_GROWS_ON.contains(belowState.getBlock());
        }
        else
        {
            return belowState.is(this) && belowState.getValue(PART) == DoubleCropBlock.Part.BOTTOM;
        }
    }

    public void placeTwoHalves(LevelAccessor level, BlockPos pos, int flags)
    {
        level.setBlock(pos, defaultBlockState().setValue(PART, DoubleCropBlock.Part.BOTTOM), flags);
        level.setBlock(pos.above(), defaultBlockState().setValue(PART, DoubleCropBlock.Part.TOP), flags);
    }
}
