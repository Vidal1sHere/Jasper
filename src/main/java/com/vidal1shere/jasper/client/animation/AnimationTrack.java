package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.animation.keyframe.BoneSampler;
import com.vidal1shere.jasper.client.animation.keyframe.BoneTimeline;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.MolangContext;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class AnimationTrack {
    // Bounds the stack when animations arrive faster than they fade
    public static final int MAX_FADES = 8;

    // Each play is its own entry, so the same animation played again is a different one
    public record Entry(@Nullable ResourceLocation id, long startTick, float speed, boolean loop) {
        // Never wrapped, so the cycles of a loop can be counted
        public float elapsed(double now) {
            return (float) Math.max(0.0, (now - startTick) / 20.0 * speed);
        }

        public float seconds(double now, BoneTimeline animation) {
            float seconds = elapsed(now);
            return loops(animation) && animation.lengthInSeconds() > 0.0F ? seconds % animation.lengthInSeconds() : seconds;
        }

        public boolean loops(BoneTimeline animation) {
            return loop && animation.loop() == BoneTimeline.LoopType.PLAY_ONCE;
        }

        public boolean cycles(BoneTimeline animation) {
            return animation.loop() == BoneTimeline.LoopType.LOOP || loops(animation);
        }
    }

    private record Fade(Entry entry, double fadeStart, float fadeTicks) {
    }

    private final JasperAnimationLoader loader;
    private final List<Fade> fades = new ArrayList<>();
    private final float[] blends = new float[MAX_FADES];

    public AnimationTrack(JasperAnimationLoader loader) {
        this.loader = loader;
        fades.add(new Fade(new Entry(null, 0L, 1.0F, false), 0.0, 0.0F));
    }

    public void set(@Nullable ResourceLocation id, long startTick, float speed, boolean loop) {
        JasperAnimation fading = id == null ? animation(fades.size() - 1) : loader.get(id);
        push(new Entry(id, startTick, speed, loop), startTick, fading == null ? 0.0F : fading.keyframes().blendTicks());
    }

    // Ends a finished play once animation with an empty fade at its end, then drops the fades hidden by a fully faded one
    public void update(double now) {
        Fade top = fades.getLast();
        JasperAnimation animation = animation(fades.size() - 1);
        if (animation != null && animation.keyframes().loop() == BoneTimeline.LoopType.PLAY_ONCE && !top.entry().loops(animation.keyframes())) {
            double end = top.entry().startTick() + animation.keyframes().lengthInSeconds() * 20.0 / top.entry().speed();
            if (now >= end) {
                push(new Entry(null, (long) end, 1.0F, false), end, animation.keyframes().blendTicks());
            }
        }
        int hidden = 0;
        for (int i = 1; i < fades.size(); i++) {
            Fade fade = fades.get(i);
            float blend = fade.fadeTicks() <= 0.0F ? 1.0F : (float) Math.min(1.0, Math.max(0.0, (now - fade.fadeStart()) / fade.fadeTicks()));
            blends[i] = blend;
            if (blend >= 1.0F) {
                hidden = i;
            }
        }
        if (hidden > 0) {
            fades.subList(0, hidden).clear();
            System.arraycopy(blends, hidden, blends, 0, fades.size());
        }
        blends[0] = 1.0F;
    }

    public boolean isActive() {
        for (int i = 0; i < fades.size(); i++) {
            if (animation(i) != null) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return fades.size();
    }

    public float blend(int i) {
        return blends[i];
    }

    public Entry entry(int i) {
        return fades.get(i).entry();
    }

    // -1 once it was dropped
    public int indexOf(Entry entry) {
        for (int i = 0; i < fades.size(); i++) {
            if (fades.get(i).entry() == entry) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public JasperAnimation animation(int i) {
        ResourceLocation id = fades.get(i).entry().id();
        return id == null ? null : loader.get(id);
    }

    @Nullable
    public JasperAnimation evaluate(int i, double now, MolangContext context, AnimationPose pose) {
        pose.reset();
        JasperAnimation animation = animation(i);
        if (animation != null) {
            BoneSampler.evaluate(animation.keyframes(), fades.get(i).entry().seconds(now, animation.keyframes()), context, pose);
        }
        return animation;
    }

    private void push(Entry entry, double fadeStart, float fadeTicks) {
        if (fades.size() >= MAX_FADES) {
            fades.removeFirst();
        }
        fades.add(new Fade(entry, fadeStart, fadeTicks));
    }
}
