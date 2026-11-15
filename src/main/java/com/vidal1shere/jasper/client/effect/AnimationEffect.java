package com.vidal1shere.jasper.client.effect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// One effect type of the animation timelines, registered in JasperEffects; null when the effect is over as soon as it starts
@OnlyIn(Dist.CLIENT)
public interface AnimationEffect {
    @Nullable
    PlayingEffect start(EffectContext context);

    // A visual effect only fires where its target is seen; the others, like sounds, fire from any target in any view, so they belong in one animation only
    default boolean visual() {
        return true;
    }
}
