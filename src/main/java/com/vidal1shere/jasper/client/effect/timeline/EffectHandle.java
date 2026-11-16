package com.vidal1shere.jasper.client.effect.timeline;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.layer.LayerKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// Bound to the animation entry that fired it, or, played from outside an animation, to whoever holds it until it is ended
@OnlyIn(Dist.CLIENT)
public final class EffectHandle {
    private final PlayingEffect instance;
    private final EffectContext context;
    @Nullable
    private final LayerKey key;
    @Nullable
    private final AnimationTrack.Entry entry;
    private final long start;
    // Elapsed seconds of the entry, or seconds since the start without one
    private final float until;
    private final boolean persist;
    private boolean released;
    private boolean ended;

    EffectHandle(PlayingEffect instance, EffectContext context, @Nullable LayerKey key, @Nullable AnimationTrack.Entry entry, long start, float until, boolean persist) {
        this.instance = instance;
        this.context = context;
        this.key = key;
        this.entry = entry;
        this.start = start;
        this.until = until;
        this.persist = persist;
    }

    // Like its animation stopping: a persisting effect that ends by itself plays on, the others end now
    public void end() {
        if (ended || released) {
            return;
        }
        if (persist && instance.endsByItself()) {
            released = true;
        } else {
            discard();
        }
    }

    public boolean ended() {
        return ended;
    }

    void discard() {
        if (!ended) {
            ended = true;
            instance.end();
        }
    }

    boolean tick(long now, @Nullable ClientAnimationData layers) {
        if (ended) {
            return false;
        }
        if (!released) {
            if (!playing(layers)) {
                end();
                if (ended) {
                    return false;
                }
            } else if (!Float.isNaN(until) && !Float.isInfinite(until) && until <= elapsed(now)) {
                discard();
                return false;
            }
        }
        if (!instance.tick(context)) {
            ended = true;
            return false;
        }
        return true;
    }

    private boolean playing(@Nullable ClientAnimationData layers) {
        if (entry == null || key == null) {
            return true;
        }
        LayerState layer = layers == null ? null : layers.existing(key.target(), key.layer());
        return layer != null && layer.track().entry(layer.track().size() - 1) == entry;
    }

    private float elapsed(long now) {
        return entry != null ? entry.elapsed(now) : (now - start) / 20.0F;
    }
}
