package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class FovModifiers {
    private static final List<Change> ACTIVE = new ArrayList<>();

    static Change start(FovEffect effect, boolean bound, long now) {
        Change change = new Change(effect, bound, now);
        ACTIVE.add(change);
        return change;
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !JasperClientConfig.FOV.get()) {
            ACTIVE.clear();
            return;
        }
        long now = level.getGameTime();
        ACTIVE.removeIf(change -> change.over(now));
        float modifier = event.getNewFovModifier();
        for (Change change : ACTIVE) {
            modifier *= 1.0F + change.effect.amount() * change.strength(now);
        }
        event.setNewFovModifier(modifier);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    static final class Change implements PlayingEffect {
        private final FovEffect effect;
        private final long start;
        // When the fade out begins; unknown until a bound change is released
        private long released;

        private Change(FovEffect effect, boolean bound, long start) {
            this.effect = effect;
            this.start = start;
            this.released = bound ? -1L : start + (long) Math.ceil(effect.fadeIn().duration() + effect.duration());
        }

        @Override
        public boolean tick(EffectContext context) {
            return !over(context.entity().level().getGameTime());
        }

        @Override
        public void end() {
            ClientLevel level = Minecraft.getInstance().level;
            long now = level == null ? start : level.getGameTime();
            if (released < 0L || released > now) {
                released = now;
            }
        }

        @Override
        public boolean endsByItself() {
            return released >= 0L;
        }

        private float strength(long now) {
            float in = effect.fadeIn().progress(now - start);
            float out = released < 0L || now < released ? 1.0F : 1.0F - effect.fadeOut().progress(now - released);
            return in * out;
        }

        private boolean over(long now) {
            return released >= 0L && now >= released + effect.fadeOut().duration();
        }
    }
}
