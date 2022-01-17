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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import net.dries007.tfc.network.DataManagerSyncPacket;
import net.dries007.tfc.util.collections.IndirectHashCollection;

public class Fertilizer extends ItemDefinition
{
    public static final DataManager<Fertilizer> MANAGER = new DataManager<>("fertilizers", "fertilizer", Fertilizer::new, Fertilizer::reload, Fertilizer::new, Fertilizer::encode, DataManagerSyncPacket.TFertilizer::new);
    public static final IndirectHashCollection<Item, Fertilizer> CACHE = new IndirectHashCollection<>(Fertilizer::getValidItems);

    @Nullable
    public static Fertilizer get(ItemStack stack)
    {
        for (Fertilizer def : CACHE.getAll(stack.getItem()))
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

    private final float nitrogen, phosphorus, potassium;

    private Fertilizer(ResourceLocation id, JsonObject json)
    {
        super(id, Ingredient.fromJson(JsonHelpers.get(json, "ingredient")));

        nitrogen = JsonHelpers.getAsFloat(json, "nitrogen", 0);
        phosphorus = JsonHelpers.getAsFloat(json, "phosphorus", 0);
        potassium = JsonHelpers.getAsFloat(json, "potassium", 0);
    }

    private Fertilizer(ResourceLocation id, FriendlyByteBuf buffer)
    {
        super(id, Ingredient.fromNetwork(buffer));

        nitrogen = buffer.readFloat();
        phosphorus = buffer.readFloat();
        potassium = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer)
    {
        ingredient.toNetwork(buffer);
        buffer.writeFloat(nitrogen);
        buffer.writeFloat(phosphorus);
        buffer.writeFloat(potassium);
    }

    public float getNitrogen()
    {
        return nitrogen;
    }

    public float getPhosphorus()
    {
        return phosphorus;
    }

    public float getPotassium()
    {
        return potassium;
    }
}
