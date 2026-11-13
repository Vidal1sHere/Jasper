package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.keyframe.AnimationPlayback;
import com.vidal1shere.jasper.client.player.PlayerAnimation;
import com.vidal1shere.jasper.client.player.PlayerAnimationLoader;
import net.minecraft.world.InteractionHand;

public final class PlayerAnimationData {
    private final AnimationPlayback<PlayerAnimation> playback = new AnimationPlayback<>(PlayerAnimationLoader.INSTANCE, PlayerAnimation::animation);
    private InteractionHand hand = InteractionHand.MAIN_HAND;

    public AnimationPlayback<PlayerAnimation> playback() {
        return playback;
    }

    public InteractionHand hand() {
        return hand;
    }

    public void setHand(InteractionHand hand) {
        this.hand = hand;
    }
}
