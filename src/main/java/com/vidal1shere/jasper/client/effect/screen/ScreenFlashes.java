package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.JasperClientConfig;
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
    private static final List<ScreenFade<FlashEffect>> ACTIVE = new ArrayList<>();

    static ScreenFade<FlashEffect> start(FlashEffect effect, boolean bound) {
        ScreenFade<FlashEffect> flash = new ScreenFade<>(effect, ScreenFade.REAL_MILLIS, effect.duration(), effect.fadeIn(), effect.fadeOut(), bound);
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
        for (ScreenFade<FlashEffect> flash : ACTIVE) {
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
}
