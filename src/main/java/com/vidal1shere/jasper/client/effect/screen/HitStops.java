package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.JasperClientConfig;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

// The whole client freezes like a lag spike, then the timer catches up the frozen ticks, so it is never behind the server
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class HitStops {
    private static long frozenUntil;

    static void start(float duration) {
        long length = Math.round(duration * JasperClientConfig.HIT_STOP_SCALE.get());
        if (length > 0L) {
            // Overlapping hit stops do not add up: a new one only extends the current one
            frozenUntil = Math.max(frozenUntil, Util.getMillis() + length);
        }
    }

    public static boolean isFrozen() {
        return Util.getMillis() < frozenUntil;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        frozenUntil = 0L;
    }
}
