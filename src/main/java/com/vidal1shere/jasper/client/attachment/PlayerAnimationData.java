package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import net.minecraft.world.InteractionHand;

public final class PlayerAnimationData {
    private final AnimationTrack track = new AnimationTrack(JasperAnimationLoader.PLAYER);
    private final AnimationTrack overlay = new AnimationTrack(JasperAnimationLoader.PLAYER);
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    private InteractionHand overlayHand = InteractionHand.MAIN_HAND;

    public AnimationTrack track() {
        return track;
    }

    public InteractionHand hand() {
        return hand;
    }

    public void setHand(InteractionHand hand) {
        this.hand = hand;
    }

    public AnimationTrack overlay() {
        return overlay;
    }

    public InteractionHand overlayHand() {
        return overlayHand;
    }

    public void setOverlayHand(InteractionHand overlayHand) {
        this.overlayHand = overlayHand;
    }
}
