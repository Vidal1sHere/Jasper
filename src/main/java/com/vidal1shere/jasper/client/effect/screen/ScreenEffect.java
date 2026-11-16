package com.vidal1shere.jasper.client.effect.screen;

import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// Only the effects of the local player's own animations reach its screen
@OnlyIn(Dist.CLIENT)
public interface ScreenEffect extends AnimationEffect {
    boolean enabled();

    @Nullable
    PlayingEffect play(EffectContext context);

    @Override
    default boolean visual() {
        return false;
    }

    @Override
    @Nullable
    default PlayingEffect start(EffectContext context) {
        return context.entity() == Minecraft.getInstance().player && enabled() ? play(context) : null;
    }
}
