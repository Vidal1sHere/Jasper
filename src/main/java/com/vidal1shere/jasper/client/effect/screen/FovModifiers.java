package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.JasperClientConfig;
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
    private static final List<ScreenFade<FovEffect>> ACTIVE = new ArrayList<>();

    static ScreenFade<FovEffect> start(FovEffect effect, boolean bound) {
        ScreenFade<FovEffect> change = new ScreenFade<>(effect, ScreenFade.GAME_TICKS, effect.duration(), effect.fadeIn(), effect.fadeOut(), bound);
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
        for (ScreenFade<FovEffect> change : ACTIVE) {
            modifier *= 1.0F + change.effect.amount() * change.strength(now);
        }
        event.setNewFovModifier(modifier);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }
}
