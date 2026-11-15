package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.animation.keyframe.BoneSampler;
import com.vidal1shere.jasper.client.animation.pose.AnimationPose;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.client.molang.MolangQuery;
import com.vidal1shere.jasper.layer.PlayOptions;
import com.vidal1shere.jasper.layer.PlayView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class AnimationTrack {
    public static final int MAX_FADES = 8;

    public record Entry(@Nullable ResourceLocation id, long startTick, float speed, boolean loop, @Nullable ResourceLocation firstPerson, PlayView view) {
        // Never wrapped, so the cycles of a loop can be counted
        public float elapsed(double now) {
            return (float) Math.max(0.0, (now - startTick) / 20.0 * speed);
        }

        public float seconds(double now, JasperAnimation animation) {
            float seconds = elapsed(now);
            return loops(animation) && animation.length() > 0.0F ? seconds % animation.length() : seconds;
        }

        public boolean loops(JasperAnimation animation) {
            return loop && animation.loop() == JasperAnimation.LoopType.PLAY_ONCE;
        }

        public boolean cycles(JasperAnimation animation) {
            return animation.loop() == JasperAnimation.LoopType.LOOP || loops(animation);
        }
    }

    private record Fade(Entry entry, double fadeStart, float fadeTicks) {
    }

    private final JasperAnimationLoader loader;
    private final List<Fade> fades = new ArrayList<>();
    private final float[] blends = new float[MAX_FADES];

    public AnimationTrack(JasperAnimationLoader loader) {
        this.loader = loader;
        fades.add(new Fade(new Entry(null, 0L, 1.0F, false, null, PlayView.ALL), 0.0, 0.0F));
    }

    public void set(@Nullable ResourceLocation id, long startTick, float speed, boolean loop) {
        set(id, startTick, new PlayOptions(speed, InteractionHand.MAIN_HAND, loop));
    }

    public void set(@Nullable ResourceLocation id, long startTick, PlayOptions options) {
        JasperAnimation fading = id == null ? animation(fades.size() - 1) : loader.get(id);
        push(new Entry(id, startTick, options.speed(), options.loop(), options.firstPerson(), options.view()), startTick, fading == null ? 0.0F : fading.blendTicks());
    }

    public void update(double now) {
        Fade top = fades.getLast();
        JasperAnimation animation = animation(fades.size() - 1);
        if (animation != null && animation.loop() == JasperAnimation.LoopType.PLAY_ONCE && !top.entry().loops(animation)) {
            double end = top.entry().startTick() + animation.length() * 20.0 / top.entry().speed();
            if (now >= end) {
                push(new Entry(null, (long) end, 1.0F, false, null, PlayView.ALL), end, animation.blendTicks());
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
    public JasperAnimation animation(int i, boolean firstPerson) {
        Entry entry = fades.get(i).entry();
        if (entry.view() == (firstPerson ? PlayView.THIRD_PERSON : PlayView.FIRST_PERSON)) {
            return null;
        }
        if (firstPerson && entry.firstPerson() != null) {
            JasperAnimation variant = loader.get(entry.firstPerson());
            if (variant != null) {
                return variant;
            }
        }
        return animation(i);
    }

    // The view comes from the is_first_person query of the context
    @Nullable
    public JasperAnimation evaluate(int i, double now, MolangContext context, AnimationPose pose) {
        pose.reset();
        JasperAnimation animation = animation(i, context.query(MolangQuery.IS_FIRST_PERSON) > 0.5F);
        if (animation != null) {
            BoneSampler.evaluate(animation, fades.get(i).entry().seconds(now, animation), context, pose);
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
