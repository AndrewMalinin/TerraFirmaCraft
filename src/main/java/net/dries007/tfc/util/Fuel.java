/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.dries007.tfc.network.DataManagerSyncPacket;
import net.dries007.tfc.util.collections.IndirectHashCollection;

public final class Fuel extends ItemDefinition
{
    public static final DataManager<Fuel> MANAGER = new DataManager<>("fuels", "fuel", Fuel::new, Fuel::reload, Fuel::new, Fuel::encode, DataManagerSyncPacket.TFuel::new);
    public static final IndirectHashCollection<Item, Fuel> CACHE = new IndirectHashCollection<>(Fuel::getValidItems);

    @Nullable
    public static Fuel get(ItemStack stack)
    {
        for (Fuel def : CACHE.getAll(stack.getItem()))
        {
            if (def.matches(stack))
            {
                return def;
            }
        }
        return null;
    }

    private static void reload()
    {
        CACHE.reload(MANAGER.getValues());
    }

    private final int duration;
    private final float temperature;

    public Fuel(ResourceLocation id, JsonObject json)
    {
        super(id, json);

        this.duration = GsonHelper.getAsInt(json, "duration");
        this.temperature = GsonHelper.getAsFloat(json, "temperature");
    }

    public Fuel(ResourceLocation id, FriendlyByteBuf buffer)
    {
        super(id, Ingredient.fromNetwork(buffer));

        this.duration = buffer.readVarInt();
        this.temperature = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer)
    {
        ingredient.toNetwork(buffer);

        buffer.writeVarInt(duration);
        buffer.writeFloat(temperature);
    }

    public int getDuration()
    {
        return duration;
    }

    public float getTemperature()
    {
        return temperature;
    }
}
