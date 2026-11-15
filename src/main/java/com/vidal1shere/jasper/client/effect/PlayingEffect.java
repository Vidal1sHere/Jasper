package com.vidal1shere.jasper.client.effect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PlayingEffect {
    boolean tick(EffectContext context);

    void end();

    // One that never ends by itself ends with its animation even when it persists
    default boolean endsByItself() {
        return true;
    }
}
