package com.vidal1shere.jasper.client.effect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PlayingEffect {
    // Every client tick; false once it is over by itself
    boolean tick(EffectContext context);

    // Cut short: its animation stopped without persist, its until came, or its entity left
    void end();

    // One that never ends by itself ends with its animation even when it persists
    default boolean endsByItself() {
        return true;
    }
}
