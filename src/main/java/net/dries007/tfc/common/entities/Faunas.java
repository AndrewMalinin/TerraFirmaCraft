/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.fmllegacy.RegistryObject;

import net.dries007.tfc.common.entities.aquatic.*;
import net.dries007.tfc.world.chunkdata.ChunkData;

public class Faunas
{
    public static final FaunaType<TFCCod> COD = registerFish(TFCEntities.COD);
    public static final FaunaType<Jellyfish> JELLYFISH = registerFish(TFCEntities.JELLYFISH);
    public static final FaunaType<TFCTropicalFish> TROPICAL_FISH = registerFish(TFCEntities.TROPICAL_FISH);
    public static final FaunaType<Bluegill> BLUEGILL = registerFish(TFCEntities.BLUEGILL);
    public static final FaunaType<TFCPufferfish> PUFFERFISH = registerFish(TFCEntities.PUFFERFISH);
    public static final FaunaType<TFCSalmon> SALMON = registerFish(TFCEntities.SALMON);
    public static final FaunaType<AquaticCritterEntity> LOBSTER = registerFish(TFCEntities.LOBSTER);
    public static final FaunaType<AquaticCritterEntity> ISOPOD = registerFish(TFCEntities.ISOPOD);
    public static final FaunaType<AquaticCritterEntity> HORSESHOE_CRAB = registerFish(TFCEntities.HORSESHOE_CRAB);

    public static void registerSpawnPlacements()
    {
        registerSpawnPlacement(COD);
        registerSpawnPlacement(JELLYFISH);
        registerSpawnPlacement(TROPICAL_FISH);
        registerSpawnPlacement(BLUEGILL);
        registerSpawnPlacement(PUFFERFISH);
        registerSpawnPlacement(SALMON);
        registerSpawnPlacement(LOBSTER);
        registerSpawnPlacement(ISOPOD);
        registerSpawnPlacement(HORSESHOE_CRAB);
    }

    private static <E extends Mob> FaunaType<E> registerFish(RegistryObject<EntityType<E>> entity)
    {
        return register(entity, SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
    }

    private static <E extends Mob> FaunaType<E> register(RegistryObject<EntityType<E>> entity, SpawnPlacements.Type spawnPlacement, Heightmap.Types heightmapType)
    {
        final Supplier<Fauna> fauna = Fauna.MANAGER.register(entity.getId());
        return new FaunaType<>(entity, fauna, spawnPlacement, heightmapType);
    }

    private static <E extends Mob> void registerSpawnPlacement(FaunaType<E> type)
    {
        SpawnPlacements.register(type.entity().get(), type.spawnPlacementType(), type.heightmapType(), (mob, level, heightmap, pos, rand) -> {
            final Fauna fauna = type.fauna().get();
            if (rand.nextInt(fauna.getChance()) != 0)
            {
                return false;
            }

            if (mob instanceof AquaticMob aquaticMob && !aquaticMob.canSpawnIn(level.getFluidState(pos).getType()))
            {
                return false;
            }

            final int seaLevel = level.getLevel().getChunkSource().getGenerator().getSeaLevel();
            if (fauna.getDistanceBelowSeaLevel() != -1 && pos.getY() > (seaLevel - fauna.getDistanceBelowSeaLevel()))
            {
                return false;
            }

            final ChunkData data = ChunkData.get(level, pos);
            if (!fauna.getClimateConfig().isValid(data, pos, rand))
            {
                return false;
            }

            final BlockPos below = pos.below();
            return !fauna.isSolidGround() || level.getBlockState(below).is(BlockTags.VALID_SPAWN);
        });
    }

    record FaunaType<E extends Mob>(Supplier<EntityType<E>> entity, Supplier<Fauna> fauna, SpawnPlacements.Type spawnPlacementType, Heightmap.Types heightmapType) {}
}
