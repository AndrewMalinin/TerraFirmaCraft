/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util.collections;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.server.ServerLifecycleHooks;

import net.dries007.tfc.util.CacheInvalidationListener;

/**
 * This is a structure which provides O(1), {@link HashMap} access of the wrapped {@code Map<Predicate<V>, R>}
 * It does this by using the "indirect key", or {@code K}. By using two constructs:
 * - The key mapper, a {@code Function<V, K>}.
 * - And the key extractor, a{@code Function<R, Iterable<K>>}.
 * The above must satisfy the following condition:
 * - For any K, V, R, {@code (keyMapper.apply(V) == K) -> (keyExtractor.apply(R).contains(K))}
 * <p>
 * This was benchmarked using VisualVM, with a recipe list of ~1000 recipes (not uncommon), using Landslide Recipes, over >10,000 invocations.
 * - Vanilla's recipe manager query took 847 us / recipe
 * - Using a LRU cache of size 1, delegating to the above took 273 us / recipe
 * - this took 11 us / recipe.
 */
public class IndirectHashCollection<K, R>
{
    public static <K, R> IndirectHashCollection<K, R> create(Function<R, Iterable<? extends K>> keyExtractor, Supplier<Collection<R>> reloadableCollection)
    {
        return new IndirectHashCollection<>(keyExtractor, reloadableCollection);
    }

    public static <C extends Container, K, R extends Recipe<C>> IndirectHashCollection<K, R> createForRecipe(Function<R, Iterable<? extends K>> keyExtractor, Supplier<RecipeType<R>> recipeType)
    {
        return new IndirectHashCollection<>(keyExtractor, () -> ServerLifecycleHooks.getCurrentServer().getRecipeManager().getAllRecipesFor(recipeType.get()));
    }

    private final Map<K, Collection<R>> indirectResultMap;
    private final Function<R, Iterable<? extends K>> keyExtractor;
    private final Supplier<Collection<R>> reloadableCollection;
    private boolean valid;

    private IndirectHashCollection(Function<R, Iterable<? extends K>> keyExtractor, Supplier<Collection<R>> reloadableCollection)
    {
        this.keyExtractor = keyExtractor;
        this.indirectResultMap = new HashMap<>();
        this.reloadableCollection = reloadableCollection;
        this.valid = false;

        CacheInvalidationListener.INSTANCE.doOnInvalidate(this::invalidate);
    }

    /**
     * This is implemented for convenience rather than add / clear methods.
     */
    public void reload(Collection<R> results)
    {
        indirectResultMap.clear();
        results.forEach(result -> {
            for (K directKey : keyExtractor.apply(result))
            {
                indirectResultMap.computeIfAbsent(directKey, k -> new ArrayList<>()).add(result);
            }
        });
    }

    public Collection<R> getAll(K key)
    {
        if (!valid)
        {
            valid = true;
            indirectResultMap.clear();
            reloadableCollection.get().forEach(result -> {
                for (K directKey : keyExtractor.apply(result))
                {
                    indirectResultMap.computeIfAbsent(directKey, k -> new ArrayList<>()).add(result);
                }
            });
        }
        return indirectResultMap.getOrDefault(key, Collections.emptyList());
    }

    public void invalidate()
    {
        valid = false;
    }
}