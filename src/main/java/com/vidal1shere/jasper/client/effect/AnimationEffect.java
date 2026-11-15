package com.vidal1shere.jasper.client.effect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Start returns null when the effect is over as soon as it starts
@OnlyIn(Dist.CLIENT)
public interface AnimationEffect {
    @Nullable
    PlayingEffect start(EffectContext context);

    // Non visual effects, like sounds, fire from any target in any view, so they belong in one animation only
    default boolean visual() {
        return true;
    }

    default List<String> locators() {
        return List.of();
    }

    default List<String> bones() {
        return List.of();
    }
}
