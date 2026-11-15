package com.vidal1shere.jasper.client.effect.timeline;

import com.vidal1shere.jasper.client.effect.AnimationEffect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

// Until is NO_UNTIL, UNTIL_END or a time of the same animation, in seconds; molang is null when every number of the effect is constant
@OnlyIn(Dist.CLIENT)
public record EffectEntry(float time, AnimationEffect effect, @Nullable MolangEffect molang, @Nullable String locator, boolean follow, float until, boolean persist) {
    public static final float NO_UNTIL = Float.NaN;
    public static final float UNTIL_END = Float.POSITIVE_INFINITY;
}
