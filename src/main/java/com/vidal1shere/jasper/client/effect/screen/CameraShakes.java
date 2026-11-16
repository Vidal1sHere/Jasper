package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.JasperClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class CameraShakes {
    private static final List<Shake> ACTIVE = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    static ScreenFade<CameraShakeEffect> start(CameraShakeEffect effect, boolean bound) {
        ScreenFade<CameraShakeEffect> fade = new ScreenFade<>(effect, ScreenFade.GAME_TICKS, effect.duration(), effect.fadeIn(), effect.fadeOut(), bound);
        ACTIVE.add(new Shake(fade, RANDOM.nextFloat() * Mth.TWO_PI));
        return fade;
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !JasperClientConfig.CAMERA_SHAKE.get()) {
            ACTIVE.clear();
            return;
        }
        double now = minecraft.level.getGameTime() + event.getPartialTick();
        float scale = minecraft.options.screenEffectScale().get().floatValue();
        float pitch = 0.0F;
        float yaw = 0.0F;
        float roll = 0.0F;
        ACTIVE.removeIf(shake -> shake.fade.over(now));
        for (Shake shake : ACTIVE) {
            ScreenFade<CameraShakeEffect> fade = shake.fade;
            float amplitude = fade.effect.intensity() * fade.strength(now) * scale;
            double phase = (now - fade.start) / 20.0 * fade.effect.frequency() * Mth.TWO_PI + shake.seed;
            pitch += amplitude * (float) Math.sin(phase);
            yaw += amplitude * 0.6F * (float) Math.sin(phase * 1.37 + 1.0);
            roll += amplitude * 0.4F * (float) Math.sin(phase * 0.71 + 2.0);
        }
        event.setPitch(event.getPitch() + pitch);
        event.setYaw(event.getYaw() + yaw);
        event.setRoll(event.getRoll() + roll);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    private record Shake(ScreenFade<CameraShakeEffect> fade, float seed) {
    }
}
