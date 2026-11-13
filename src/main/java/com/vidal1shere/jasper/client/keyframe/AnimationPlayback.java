package com.vidal1shere.jasper.client.keyframe;

import com.vidal1shere.jasper.client.keyframe.serialization.KeyframeAnimationLoader;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public final class AnimationPlayback<T> {
    public record Entry(@Nullable ResourceLocation id, long startTick, float speed) {
        public static final Entry NONE = new Entry(null, 0L, 1.0F);

        public float seconds(double now) {
            return (float) Math.max(0.0, (now - startTick) / 20.0 * speed);
        }
    }

    private final KeyframeAnimationLoader<T> loader;
    private final Function<T, KeyframeAnimation> animation;
    private Entry current = Entry.NONE;
    private Entry previous = Entry.NONE;

    public AnimationPlayback(KeyframeAnimationLoader<T> loader, Function<T, KeyframeAnimation> animation) {
        this.loader = loader;
        this.animation = animation;
    }

    public void set(@Nullable ResourceLocation id, long startTick, float speed) {
        previous = current;
        current = new Entry(id, startTick, speed);
    }

    public Entry current() {
        return current;
    }

    public Entry previous() {
        return previous;
    }

    @Nullable
    public T definition(Entry entry) {
        return entry.id() == null ? null : loader.get(entry.id());
    }

    @Nullable
    public KeyframeAnimation animation(Entry entry) {
        T definition = definition(entry);
        return definition == null ? null : animation.apply(definition);
    }

    public boolean running(Entry entry, double now) {
        KeyframeAnimation keyframes = animation(entry);
        return keyframes != null && !keyframes.finished(entry.seconds(now));
    }

    public float weight(double now) {
        KeyframeAnimation keyframes = animation(current);
        if (keyframes == null) {
            keyframes = animation(previous);
        }
        float blendTicks = keyframes == null ? 0.0F : keyframes.blendTicks();
        return blendTicks <= 0.0F ? 1.0F : (float) Math.min(1.0, Math.max(0.0, (now - current.startTick()) / blendTicks));
    }

    // Weight left while a finished play once entry fades out
    public float fadeOut(Entry entry, double now) {
        KeyframeAnimation keyframes = animation(entry);
        if (keyframes == null || keyframes.loop() != KeyframeAnimation.LoopType.PLAY_ONCE || keyframes.blendTicks() <= 0.0F) {
            return 0.0F;
        }
        double end = entry.startTick() + keyframes.lengthInSeconds() * 20.0 / entry.speed();
        if (now < end) {
            return 0.0F;
        }
        return (float) Math.max(0.0, 1.0 - (now - end) / keyframes.blendTicks());
    }

    @Nullable
    public T evaluateLastFrame(Entry entry, MolangContext context, AnimationPose pose) {
        pose.reset();
        T definition = definition(entry);
        if (definition != null) {
            KeyframeAnimation keyframes = animation.apply(definition);
            KeyframeAnimations.evaluate(keyframes, keyframes.lengthInSeconds(), context, pose);
        }
        return definition;
    }

    public boolean isActive(double now) {
        return running(current, now) || weight(now) < 1.0F && running(previous, now);
    }

    @Nullable
    public T evaluate(Entry entry, double now, MolangContext context, AnimationPose pose) {
        pose.reset();
        T definition = definition(entry);
        if (definition == null) {
            return null;
        }
        KeyframeAnimation keyframes = animation.apply(definition);
        float seconds = entry.seconds(now);
        if (keyframes.finished(seconds)) {
            return null;
        }
        KeyframeAnimations.evaluate(keyframes, seconds, context, pose);
        return definition;
    }
}
