package com.vidal1shere.jasper.layer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

/*
 * The hand only mirrors: an animation written for the right arm plays on the left one when the hand is on that side.
 * Loop makes a play once animation start over at its end; looping and held animations keep what their file says.
 * The first person animation is timed by the played one.
 */
public record PlayOptions(float speed, InteractionHand hand, boolean loop, @Nullable ResourceLocation firstPerson, PlayView view) {
    public static final PlayOptions DEFAULT = new PlayOptions(1.0F, InteractionHand.MAIN_HAND, false);

    public PlayOptions(float speed, InteractionHand hand, boolean loop) {
        this(speed, hand, loop, null, PlayView.ALL);
    }

    public PlayOptions withSpeed(float speed) {
        return new PlayOptions(speed, hand, loop, firstPerson, view);
    }

    public PlayOptions withHand(InteractionHand hand) {
        return new PlayOptions(speed, hand, loop, firstPerson, view);
    }

    public PlayOptions withLoop(boolean loop) {
        return new PlayOptions(speed, hand, loop, firstPerson, view);
    }

    public PlayOptions withFirstPerson(@Nullable ResourceLocation firstPerson) {
        return new PlayOptions(speed, hand, loop, firstPerson, view);
    }

    public PlayOptions withView(PlayView view) {
        return new PlayOptions(speed, hand, loop, firstPerson, view);
    }
}
