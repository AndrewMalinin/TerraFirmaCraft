/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.capabilities.forge;

import org.jetbrains.annotations.Nullable;

import static net.dries007.tfc.common.capabilities.forge.ForgeStep.*;

public enum ForgeRule
{
    HIT_ANY(Order.ANY, HIT_LIGHT),
    HIT_NOT_LAST(Order.NOT_LAST, HIT_LIGHT),
    HIT_LAST(Order.LAST, HIT_LIGHT),
    HIT_SECOND_LAST(Order.SECOND_LAST, HIT_LIGHT),
    HIT_THIRD_LAST(Order.THIRD_LAST, HIT_LIGHT),
    DRAW_ANY(Order.ANY, DRAW),
    DRAW_LAST(Order.LAST, DRAW),
    DRAW_NOT_LAST(Order.NOT_LAST, DRAW),
    DRAW_SECOND_LAST(Order.SECOND_LAST, DRAW),
    DRAW_THIRD_LAST(Order.THIRD_LAST, DRAW),
    PUNCH_ANY(Order.ANY, PUNCH),
    PUNCH_LAST(Order.LAST, PUNCH),
    PUNCH_NOT_LAST(Order.NOT_LAST, PUNCH),
    PUNCH_SECOND_LAST(Order.SECOND_LAST, PUNCH),
    PUNCH_THIRD_LAST(Order.THIRD_LAST, PUNCH),
    BEND_ANY(Order.ANY, BEND),
    BEND_LAST(Order.LAST, BEND),
    BEND_NOT_LAST(Order.NOT_LAST, BEND),
    BEND_SECOND_LAST(Order.SECOND_LAST, BEND),
    BEND_THIRD_LAST(Order.THIRD_LAST, BEND),
    UPSET_ANY(Order.ANY, UPSET),
    UPSET_LAST(Order.LAST, UPSET),
    UPSET_NOT_LAST(Order.NOT_LAST, UPSET),
    UPSET_SECOND_LAST(Order.SECOND_LAST, UPSET),
    UPSET_THIRD_LAST(Order.THIRD_LAST, UPSET),
    SHRINK_ANY(Order.ANY, SHRINK),
    SHRINK_LAST(Order.LAST, SHRINK),
    SHRINK_NOT_LAST(Order.NOT_LAST, SHRINK),
    SHRINK_SECOND_LAST(Order.SECOND_LAST, SHRINK),
    SHRINK_THIRD_LAST(Order.THIRD_LAST, SHRINK);

    private static final ForgeRule[] VALUES = values();

    @Nullable
    public static ForgeRule valueOf(int id)
    {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }

    private final Order order;
    private final ForgeStep type;

    ForgeRule(Order order, ForgeStep type)
    {
        this.order = order;
        this.type = type;

        assert type != HIT_MEDIUM && type != HIT_HARD;
    }

    public int getU()
    {
        return type == HIT_LIGHT ? 218 : type.getU();
    }

    public int getV()
    {
        return type == HIT_LIGHT ? 18 : type.getV();
    }

    public int getW()
    {
        return order.v;
    }

    public boolean matches(ForgeSteps steps)
    {
        return switch (order)
            {
                case ANY -> matches(steps.getStep(2)) || matches(steps.getStep(1)) || matches(steps.getStep(0));
                case NOT_LAST -> matches(steps.getStep(1)) || matches(steps.getStep(0));
                case LAST -> matches(steps.getStep(2));
                case SECOND_LAST -> matches(steps.getStep(1));
                case THIRD_LAST -> matches(steps.getStep(0));
            };
    }

    private boolean matches(@Nullable ForgeStep step)
    {
        if (this.type == HIT_LIGHT)
        {
            return step == HIT_LIGHT || step == HIT_MEDIUM || step == HIT_HARD;
        }
        return type == step;
    }

    private enum Order
    {
        ANY(88),
        LAST(0),
        NOT_LAST(66),
        SECOND_LAST(22),
        THIRD_LAST(44);

        private final int v;

        Order(int v)
        {
            this.v = v;
        }
    }
}