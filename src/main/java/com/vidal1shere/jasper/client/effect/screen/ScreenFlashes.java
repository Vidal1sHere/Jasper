package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class ScreenFlashes {
    private static final List<Flash> ACTIVE = new ArrayList<>();

    static Flash start(FlashEffect effect, boolean bound) {
        Flash flash = new Flash(effect, bound, Util.getMillis());
        ACTIVE.add(flash);
        return flash;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        if (!JasperClientConfig.FLASH.get()) {
            ACTIVE.clear();
            return;
        }
        long now = Util.getMillis();
        GuiGraphics graphics = event.getGuiGraphics();
        ACTIVE.removeIf(flash -> flash.over(now));
        for (Flash flash : ACTIVE) {
            int alpha = Math.round(flash.effect.opacity() * flash.strength(now) * 255.0F);
            if (alpha > 0) {
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | flash.effect.color().getValue() & 0xFFFFFF);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    static final class Flash implements PlayingEffect {
        private final FlashEffect effect;
        private final long start;
        private long released;

        private Flash(FlashEffect effect, boolean bound, long start) {
            this.effect = effect;
            this.start = start;
            this.released = bound ? -1L : start + (long) (effect.fadeIn().duration() + effect.duration());
        }

        @Override
        public boolean tick(EffectContext context) {
            return !over(Util.getMillis());
        }

        @Override
        public void end() {
            long now = Util.getMillis();
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
