package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.client.effect.spec.Fade;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.LongSupplier;

@OnlyIn(Dist.CLIENT)
final class ScreenFade<E> implements PlayingEffect {
    static final LongSupplier REAL_MILLIS = Util::getMillis;
    static final LongSupplier GAME_TICKS = () -> {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    };

    final E effect;
    private final LongSupplier clock;
    private final Fade fadeIn;
    private final Fade fadeOut;
    final long start;
    private long released;

    ScreenFade(E effect, LongSupplier clock, float duration, Fade fadeIn, Fade fadeOut, boolean bound) {
        this.effect = effect;
        this.clock = clock;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
        this.start = clock.getAsLong();
        this.released = bound ? -1L : start + (long) Math.ceil(fadeIn.duration() + duration);
    }

    @Override
    public boolean tick(EffectContext context) {
        return !over(clock.getAsLong());
    }

    @Override
    public void end() {
        long now = clock.getAsLong();
        if (released < 0L || released > now) {
            released = now;
        }
    }

    @Override
    public boolean endsByItself() {
        return released >= 0L;
    }

    float strength(double now) {
        float in = fadeIn.progress(now - start);
        float out = released < 0L || now < released ? 1.0F : 1.0F - fadeOut.progress(now - released);
        return in * out;
    }

    boolean over(double now) {
        return released >= 0L && now >= released + fadeOut.duration();
    }
}
