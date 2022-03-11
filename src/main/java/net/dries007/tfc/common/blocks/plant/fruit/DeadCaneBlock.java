/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.plant.fruit;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.ICalendar;

public class DeadCaneBlock extends SpreadingCaneBlock implements EntityBlockExtension
{
    public DeadCaneBlock(ExtendedProperties properties)
    {
        super(properties, () -> Items.AIR, new Lifecycle[12], () -> Blocks.AIR, 0, 0);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random random)
    {
        if (random.nextInt(15) == 0 && level.isEmptyBlock(pos.above()))
        {
            TickCounterBlockEntity te = Helpers.getBlockEntity(level, pos, TickCounterBlockEntity.class);
            if (te != null)
            {
                if (te.getTicksSinceUpdate() > ICalendar.TICKS_IN_DAY * 80)
                {
                    if (!Helpers.isBlock(level.getBlockState(pos.above()), TFCTags.Blocks.ANY_SPREADING_BUSH))
                    {
                        te.setRemoved();
                        level.destroyBlock(pos, true);
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        return InteractionResult.FAIL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(STAGE, FACING);
    }
}
