package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

public final class LayerState {
    private final AnimationTrack track;
    private InteractionHand hand = InteractionHand.MAIN_HAND;
    @Nullable
    private ResourceLocation driven;
    @Nullable
    private ResourceLocation played;
    private long playedStart;
    private PlayOptions playedOptions = PlayOptions.DEFAULT;
    private boolean driverSeen;
    @Nullable
    private AnimationTrack.Entry firedEntry;
    private float firedTime;

    public LayerState(JasperAnimationLoader loader) {
        this.track = new AnimationTrack(loader);
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

    // The last animation a driver put on this layer, so it only pushes a change
    @Nullable
    public ResourceLocation driven() {
        return driven;
    }

    public void setDriven(@Nullable ResourceLocation driven) {
        this.driven = driven;
        driverSeen = true;
    }

    // What a play request put on this layer, which a driver leaves alone while it is alive
    @Nullable
    public ResourceLocation played() {
        return played;
    }

    public long playedStart() {
        return playedStart;
    }

    public PlayOptions playedOptions() {
        return playedOptions;
    }

    // On a driven layer a stop hands over to the driver, which crossfades from the play instead of from an empty pose
    public void play(@Nullable ResourceLocation animation, long startTick, PlayOptions options) {
        if (animation == null && driverSeen) {
            endPlay();
            return;
        }
        played = animation;
        playedStart = startTick;
        playedOptions = options;
        track.set(animation, startTick, options.speed(), options.loop());
    }

    // The entry whose effects fired last and how far, in elapsed seconds
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
    }
}
