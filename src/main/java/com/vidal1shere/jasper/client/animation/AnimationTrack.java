package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimation;
import com.vidal1shere.jasper.client.animation.keyframe.KeyframeAnimations;
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
    public static final int MAX_LAYERS = 8;

    private record Entry(@Nullable ResourceLocation id, long startTick, float speed) {
        public float seconds(double now) {
            return (float) Math.max(0.0, (now - startTick) / 20.0 * speed);
        }
    }

    private record Layer(Entry entry, double fadeStart, float fadeTicks) {
    }

    private final JasperAnimationLoader loader;
    private final List<Layer> layers = new ArrayList<>();
    private final float[] blends = new float[MAX_LAYERS];

    public AnimationTrack(JasperAnimationLoader loader) {
        this.loader = loader;
        layers.add(new Layer(new Entry(null, 0L, 1.0F), 0.0, 0.0F));
    }

    public void set(@Nullable ResourceLocation id, long startTick, float speed) {
        JasperAnimation fading = id == null ? animation(layers.size() - 1) : loader.get(id);
        push(new Entry(id, startTick, speed), startTick, fading == null ? 0.0F : fading.keyframes().blendTicks());
    }

    // Ends a finished play once animation with an empty layer at its end, then drops the layers hidden by a fully faded one
    public void update(double now) {
        Layer top = layers.getLast();
        JasperAnimation animation = animation(layers.size() - 1);
        if (animation != null && animation.keyframes().loop() == KeyframeAnimation.LoopType.PLAY_ONCE) {
            double end = top.entry().startTick() + animation.keyframes().lengthInSeconds() * 20.0 / top.entry().speed();
            if (now >= end) {
                push(new Entry(null, (long) end, 1.0F), end, animation.keyframes().blendTicks());
            }
        }
        int hidden = 0;
        for (int i = 1; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            float blend = layer.fadeTicks() <= 0.0F ? 1.0F : (float) Math.min(1.0, Math.max(0.0, (now - layer.fadeStart()) / layer.fadeTicks()));
            blends[i] = blend;
            if (blend >= 1.0F) {
                hidden = i;
            }
        }
        if (hidden > 0) {
            layers.subList(0, hidden).clear();
            System.arraycopy(blends, hidden, blends, 0, layers.size());
        }
        blends[0] = 1.0F;
    }

    public boolean isActive() {
        for (int i = 0; i < layers.size(); i++) {
            if (animation(i) != null) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return layers.size();
    }

    public float blend(int i) {
        return blends[i];
    }

    @Nullable
    public JasperAnimation animation(int i) {
        ResourceLocation id = layers.get(i).entry().id();
        return id == null ? null : loader.get(id);
    }

    @Nullable
    public JasperAnimation evaluate(int i, double now, MolangContext context, AnimationPose pose) {
        pose.reset();
        JasperAnimation animation = animation(i);
        if (animation != null) {
            KeyframeAnimations.evaluate(animation.keyframes(), layers.get(i).entry().seconds(now), context, pose);
        }
        return animation;
    }

    private void push(Entry entry, double fadeStart, float fadeTicks) {
        if (layers.size() >= MAX_LAYERS) {
            layers.removeFirst();
        }
        layers.add(new Layer(entry, fadeStart, fadeTicks));
    }
}
