package com.vidal1shere.jasper.layer;

import net.minecraft.world.InteractionHand;

// The hand only mirrors: an animation written for the right arm plays on the left one when the hand is on that side.
// Loop makes a play once animation start over at its end; looping and held animations keep what their file says.
public record PlayOptions(float speed, InteractionHand hand, boolean loop) {
    public static final PlayOptions DEFAULT = new PlayOptions(1.0F, InteractionHand.MAIN_HAND, false);

    public PlayOptions withSpeed(float speed) {
        return new PlayOptions(speed, hand, loop);
    }

    public PlayOptions withHand(InteractionHand hand) {
        return new PlayOptions(speed, hand, loop);
    }

    public PlayOptions withLoop(boolean loop) {
        return new PlayOptions(speed, hand, loop);
    }
}
