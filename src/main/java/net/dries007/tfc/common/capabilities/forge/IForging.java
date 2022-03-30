/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.capabilities.forge;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import net.dries007.tfc.common.capabilities.heat.HeatCapability;

public interface IForging extends ICapabilityProvider
{
    /**
     * Gets the current amount of work on the object
     */
    int getWork();

    /**
     * Sets the current amount of work on the object
     */
    void setWork(int work);

    /**
     * Gets the current saved recipe's registry name
     * Returns null if no recipe name is currently saved
     */
    @Nullable
    ResourceLocation getRecipeName();

    /** todo: requires anvil recipes
     * Sets the recipe name from an {@link AnvilRecipe}. If null, sets the recipe name to null
     */
    /*
    default void setRecipe(@Nullable AnvilRecipe recipe)
    {
        setRecipe(recipe != null ? recipe.getRegistryName() : null);
    }
    */

    /**
     * Sets the recipe name from an AnvilRecipe registry name.
     *
     * @param recipeName a registry name of an anvil recipe
     */
    void setRecipe(@Nullable ResourceLocation recipeName);

    /**
     * Gets the step in the last n'th position of work.
     *
     * @param index must be 0, 1, or 2, for the most recent, second, and third most recent step respectively.
     */
    @Nullable
    ForgeStep getStep(int index);

    /**
     * @param rule The rule to match
     * @return {@code true} if the current instance matches the provided rule.
     */
    boolean matches(ForgeRule rule);

    /**
     * Adds (works) a step as the new most recent, updating the other three most recent steps.
     */
    void addStep(@Nullable ForgeStep step);

    /**
     * Resets the object's {@link IForging} components. Used if an item falls out of an anvil without getting worked
     * Purpose is to preserve stackability on items that haven't been worked yet.
     */
    void reset();

    /**
     * @return true if the item is workable
     */
    default boolean canWork(ItemStack stack)
    {
        return stack.getCapability(HeatCapability.CAPABILITY).map(heat -> heat.getTemperature() > heat.getForgingTemperature()).orElse(true);
    }

    /**
     * @return true if the item is weldable
     */
    default boolean canWeld(ItemStack stack)
    {
        return stack.getCapability(HeatCapability.CAPABILITY).map(heat -> heat.getTemperature() > heat.getWeldingTemperature()).orElse(true);
    }
}