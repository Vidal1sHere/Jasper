package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class LayerState {
    private final JasperAnimationLoader loader;
    private final AnimationTrack track;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    @Nullable
    private ResourceLocation driven;
    @Nullable
    private ResourceLocation played;
    private long playedStart;
    private PlayOptions playedOptions = PlayOptions.DEFAULT;
    private boolean driverSeen;
    // After a play ends or stops, the next pick goes in even unchanged
    private boolean handOver;
    @Nullable
    private AnimationTrack.Entry firedEntry;
    private float firedTime;

    public LayerState(JasperAnimationLoader loader) {
        this.loader = loader;
        this.track = new AnimationTrack(loader);
    }

    // The client tick may update the track between the end of a play once and this call, so the fade to nothing it pushed is dropped
    public void drive(@Nullable ResourceLocation pick, double now) {
        if (played != null) {
            if (playAlive(now)) {
                return;
            }
            endPlay();
            track.dropEndFade();
        }
        if (handOver || !Objects.equals(pick, driven)) {
            track.set(pick, (long) now, 1.0F, false);
            setDriven(pick);
            handOver = false;
        }
    }

    public boolean driverSeen() {
        return driverSeen;
    }

    private boolean playAlive(double now) {
        JasperAnimation animation = loader.get(played);
        if (animation == null) {
            return false;
        }
        return playedOptions.loop() || animation.loop() != JasperAnimation.LoopType.PLAY_ONCE || now < playedStart + animation.length() * 20.0 / playedOptions.speed();
    }

    public AnimationTrack track() {
        return track;
    }

    public InteractionHand hand() {
        return hand;
    }

    public void setHand(InteractionHand hand) {
        this.hand = hand;
    }

    private void setDriven(@Nullable ResourceLocation driven) {
        this.driven = driven;
        driverSeen = true;
    }

    public void play(@Nullable ResourceLocation animation, long startTick, PlayOptions options) {
        if (animation == null && driverSeen) {
            endPlay();
            return;
        }
        played = animation;
        playedStart = startTick;
        playedOptions = options;
        track.set(animation, startTick, options);
    }

    @Nullable
    public AnimationTrack.Entry firedEntry() {
        return firedEntry;
    }

    public float firedTime() {
        return firedTime;
    }

    public void fired(AnimationTrack.Entry entry, float time) {
        firedEntry = entry;
        firedTime = time;
    }

    public void endPlay() {
        played = null;
        driven = null;
        handOver = true;
    }
}
