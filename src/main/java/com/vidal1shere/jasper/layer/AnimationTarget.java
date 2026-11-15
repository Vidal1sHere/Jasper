package com.vidal1shere.jasper.layer;

import net.minecraft.world.InteractionHand;

// MAIN_HAND and OFF_HAND animate the item held in that hand, in first person
public enum AnimationTarget {
    MODEL,
    MAIN_HAND,
    OFF_HAND;

    public static AnimationTarget of(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? MAIN_HAND : OFF_HAND;
    }
}
