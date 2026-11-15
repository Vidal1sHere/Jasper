package com.vidal1shere.jasper.mixin.client;

import com.vidal1shere.jasper.client.effect.screen.HitStops;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // Not advancing the game time runs no client tick and holds the partial tick, while packets, the camera and sounds keep going
    @ModifyArg(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DeltaTracker$Timer;advanceTime(JZ)I"), index = 1)
    private boolean jasper$hitStop(boolean advanceGameTime) {
        return advanceGameTime && !HitStops.isFrozen();
    }
}
