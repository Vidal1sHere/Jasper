package com.vidal1shere.jasper.layer;

import net.minecraft.world.InteractionHand;

public enum AnimationTarget {
    MODEL,
    MAIN_HAND,
    OFF_HAND;

    public static AnimationTarget of(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
    }
}
