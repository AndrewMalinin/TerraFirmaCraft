/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.IcePileBlock;
import net.dries007.tfc.common.blocks.SnowPileBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.ThinSpikeBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.dries007.tfc.util.tracker.WorldTrackerCapability;
import org.jetbrains.annotations.Nullable;

/**
 * This is a helper class which handles environment effects
 * It would be called by https://github.com/MinecraftForge/MinecraftForge/pull/7235, until then we simply mixin the call to our handler
 */
public final class EnvironmentHelpers
{
    public static final int ICICLE_MELT_RANDOM_TICK_CHANCE = 120; // Icicles don't melt naturally well at all, since they form under overhangs
    public static final int SNOW_MELT_RANDOM_TICK_CHANCE = 150; // Snow and ice melt naturally, but snow naturally gets placed under overhangs due to smoothing
    public static final int ICE_MELT_RANDOM_TICK_CHANCE = 400; // Ice practically never should form under overhangs, so this can be very low chance

    /**
     * Ticks a chunk for environment specific effects.
     * Handles:
     * - Placing snow while snowing, respecting snow pile-able blocks, and stacking snow.
     * - Freezing ice if cold enough, respecting freezable plants
     * - Placing icicles while snowing under overhangs
     * - Melting ice and snow due to temperature.
     */
    public static void tickChunk(ServerLevel level, LevelChunk chunk, ProfilerFiller profiler)
    {
        final ChunkPos chunkPos = chunk.getPos();
        final BlockPos lcgPos = level.getBlockRandomPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ(), 15);
        final BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, lcgPos);
        final BlockPos groundPos = surfacePos.below();
        final float temperature = Climate.getTemperature(level, surfacePos);

        final boolean rainingOrSnowing = isRainingOrSnowing(level, surfacePos);

        profiler.push("tfcSnow");
        if (rainingOrSnowing)
        {
            doSnow(level, surfacePos, temperature);
        }
        profiler.popPush("tfcIce");
        // Ice freezing doesn't require precipitation
        doIce(level, groundPos, temperature);
        profiler.popPush("tfcIcicles");
        if (rainingOrSnowing)
        {
            doIcicles(level, surfacePos, temperature);
        }
        profiler.pop();
    }

    public static boolean isSnow(BlockState state)
    {
        return Helpers.isBlock(state, Blocks.SNOW) || Helpers.isBlock(state, TFCBlocks.SNOW_PILE.get());
    }

    public static boolean isIce(BlockState state)
    {
        return Helpers.isBlock(state, Blocks.ICE) || Helpers.isBlock(state, TFCBlocks.ICE_PILE.get()) || Helpers.isBlock(state, TFCBlocks.SEA_ICE.get());
    }

    public static boolean isWater(BlockState state)
    {
        return Helpers.isBlock(state, Blocks.WATER) || Helpers.isBlock(state, TFCBlocks.SALT_WATER.get());
    }

    public static boolean isAdjacentToWater(LevelAccessor level, BlockPos pos)
    {
        return isAdjacentToMaybeWater(level, pos, true);
    }

    public static boolean isAdjacentToNotWater(LevelAccessor level, BlockPos pos)
    {
        return isAdjacentToMaybeWater(level, pos, false);
    }

    private static boolean isAdjacentToMaybeWater(LevelAccessor level, BlockPos pos, boolean expected)
    {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            if (level.isWaterAt(mutablePos.setWithOffset(pos, direction)) == expected)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isWorldgenReplaceable(WorldGenLevel level, BlockPos pos)
    {
        return isWorldgenReplaceable(level.getBlockState(pos));
    }

    public static boolean isWorldgenReplaceable(BlockState state)
    {
        return FluidHelpers.isAirOrEmptyFluid(state) || Helpers.isBlock(state, TFCTags.Blocks.SINGLE_BLOCK_REPLACEABLE);
    }

    public static boolean canPlaceBushOn(WorldGenLevel level, BlockPos pos)
    {
        return isWorldgenReplaceable(level, pos) && Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.BUSH_PLANTABLE_ON);
    }

    public static boolean isOnSturdyFace(WorldGenLevel level, BlockPos pos)
    {
        pos = pos.below();
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    public static boolean isRainingOrSnowing(Level level, BlockPos pos)
    {
        return level.isRaining() && level.getCapability(WorldTrackerCapability.CAPABILITY)
            .map(cap -> cap.isRaining(level, pos))
            .orElse(false);
    }

    private static void doSnow(Level level, BlockPos surfacePos, float temperature)
    {
        final Random random = level.getRandom();
        if (random.nextInt(16) == 0)
        {
            // Snow only accumulates during rain
            if (temperature < OverworldClimateModel.SNOW_FREEZE_TEMPERATURE && level.isRaining())
            {
                // Handle smoother snow placement: if there's an adjacent position with less snow, switch to that position instead
                // Additionally, handle up to two block tall plants if they can be piled
                // This means we need to check three levels deep
                if (!placeSnowOrSnowPile(level, surfacePos, random))
                {
                    if (!placeSnowOrSnowPile(level, surfacePos.below(), random))
                    {
                        placeSnowOrSnowPile(level, surfacePos.below(2), random);
                    }
                }
            }
            else if (temperature > OverworldClimateModel.SNOW_MELT_TEMPERATURE && random.nextInt(3) == 0)
            {
                // Snow melting - both snow and snow piles
                final BlockState state = level.getBlockState(surfacePos);
                if (isSnow(state))
                {
                    SnowPileBlock.removePileOrSnow(level, surfacePos, state);
                }
            }
        }
    }

    /**
     * @return {@code true} if a snow block or snow pile was able to be placed.
     */
    private static boolean placeSnowOrSnowPile(Level level, BlockPos initialPos, Random random)
    {
        // First, try and find an optimal position, to smoothen out snow accumulation
        final BlockPos pos = findOptimalSnowLocation(level, initialPos, level.getBlockState(initialPos), random);
        final BlockState state = level.getBlockState(pos);

        // Then, handle possibilities
        if (isSnow(state) && state.getValue(SnowLayerBlock.LAYERS) < 7)
        {
            // Snow and snow layers can accumulate snow
            final BlockState newState = state.setValue(SnowLayerBlock.LAYERS, state.getValue(SnowLayerBlock.LAYERS) + 1);
            if (newState.canSurvive(level, pos))
            {
                level.setBlock(pos, newState, 3);
            }
            return true;
        }
        else if (SnowPileBlock.canPlaceSnowPile(level, pos, state))
        {
            SnowPileBlock.placeSnowPile(level, pos, state, false);
            return true;
        }
        else if (state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(level, pos))
        {
            // Vanilla snow placement (single layers)
            level.setBlock(pos, Blocks.SNOW.defaultBlockState(), 3);
            return true;
        }
        else
        {
            // Fills cauldrons with snow
            state.getBlock().handlePrecipitation(state, level, pos, Biome.Precipitation.SNOW);
        }
        return false;
    }

    /**
     * Smoothens out snow creation so it doesn't create as uneven piles, by moving snowfall to adjacent positions where possible.
     */
    private static BlockPos findOptimalSnowLocation(LevelAccessor level, BlockPos pos, BlockState state, Random random)
    {
        BlockPos targetPos = null;
        int found = 0;
        if (isSnow(state))
        {
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                final BlockPos adjPos = pos.relative(direction);
                final BlockState adjState = level.getBlockState(adjPos);
                if ((isSnow(adjState) && adjState.getValue(SnowLayerBlock.LAYERS) < state.getValue(SnowLayerBlock.LAYERS)) // Adjacent snow that's lower than this one
                    || ((adjState.isAir() || Helpers.isBlock(adjState.getBlock(), TFCTags.Blocks.CAN_BE_SNOW_PILED)) && Blocks.SNOW.defaultBlockState().canSurvive(level, adjPos))) // Or, empty space that could support snow
                {
                    found++;
                    if (targetPos == null || random.nextInt(found) == 0)
                    {
                        targetPos = adjPos;
                    }
                }
            }
            if (targetPos != null)
            {
                return targetPos;
            }
        }
        return pos;
    }

    private static void doIce(Level level, BlockPos groundPos, float temperature)
    {
        final Random random = level.getRandom();
        BlockState groundState = level.getBlockState(groundPos);
        if (temperature < OverworldClimateModel.ICE_FREEZE_TEMPERATURE)
        {
            if (random.nextInt(16) == 0)
            {
                // First, since we want to handle water with a single block above, if we find no water, but we find one below, we choose that instead
                // However, we have to also exclude ice here, since we don't intend to freeze two layers down
                if (isIce(groundState))
                {
                    return;
                }
                if (groundState.getFluidState().getType() != Fluids.WATER)
                {
                    groundPos = groundPos.below();
                    groundState = level.getBlockState(groundPos);
                }

                IcePileBlock.placeIcePileOrIce(level, groundPos, groundState, false);
            }
        }
        else if (temperature > OverworldClimateModel.ICE_MELT_TEMPERATURE)
        {
            // Handle ice melting
            if (groundState.getBlock() == Blocks.ICE || groundState.getBlock() == TFCBlocks.ICE_PILE.get())
            {
                // Apply a heuristic to try and make ice melting more smooth, in the same way ice freezing works
                if (random.nextInt(600) == 0 || (random.nextInt(12) == 0 && isAdjacentToWater(level, groundPos)))
                {
                    IcePileBlock.removeIcePileOrIce(level, groundPos, groundState);
                }
            }
        }
    }

    private static void doIcicles(Level level, BlockPos lcgPos, float temperature)
    {
        final Random random = level.getRandom();
        if (random.nextInt(16) == 0 && level.isRaining() && temperature < OverworldClimateModel.ICICLE_MAX_FREEZE_TEMPERATURE && temperature > OverworldClimateModel.ICICLE_MIN_FREEZE_TEMPERATURE)
        {
            // Place icicles under overhangs
            final BlockPos iciclePos = findIcicleLocation(level, lcgPos, random);
            if (iciclePos != null)
            {
                BlockPos posAbove = iciclePos.above();
                BlockState stateAbove = level.getBlockState(posAbove);
                if (Helpers.isBlock(stateAbove, TFCBlocks.ICICLE.get()))
                {
                    level.setBlock(posAbove, stateAbove.setValue(ThinSpikeBlock.TIP, false), 3 | 16);
                }
                level.setBlock(iciclePos, TFCBlocks.ICICLE.get().defaultBlockState().setValue(ThinSpikeBlock.TIP, true), 3);
            }
        }
    }

    @Nullable
    private static BlockPos findIcicleLocation(Level world, BlockPos pos, Random random)
    {
        final Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos adjacentPos = pos.relative(side);
        final int adjacentHeight = world.getHeight(Heightmap.Types.MOTION_BLOCKING, adjacentPos.getX(), adjacentPos.getZ());
        BlockPos foundPos = null;
        int found = 0;
        for (int y = 0; y < adjacentHeight; y++)
        {
            final BlockState stateAt = world.getBlockState(adjacentPos);
            final BlockPos posAbove = adjacentPos.above();
            final BlockState stateAbove = world.getBlockState(posAbove);
            if (stateAt.isAir() && (stateAbove.getBlock() == TFCBlocks.ICICLE.get() || stateAbove.isFaceSturdy(world, posAbove, Direction.DOWN)))
            {
                found++;
                if (foundPos == null || random.nextInt(found) == 0)
                {
                    foundPos = adjacentPos;
                }
            }
            adjacentPos = posAbove;
        }
        return foundPos;
    }
}
