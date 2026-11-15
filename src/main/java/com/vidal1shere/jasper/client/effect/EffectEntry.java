package com.vidal1shere.jasper.client.effect;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// The fields every effect type shares; until is NO_UNTIL, UNTIL_END or a time of the same animation, in seconds
@OnlyIn(Dist.CLIENT)
public record EffectEntry(float time, AnimationEffect effect, @Nullable String locator, boolean follow, float until, boolean persist) {
    public static final float NO_UNTIL = Float.NaN;
    public static final float UNTIL_END = Float.POSITIVE_INFINITY;
}
