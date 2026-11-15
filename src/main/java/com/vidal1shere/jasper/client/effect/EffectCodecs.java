package com.vidal1shere.jasper.client.effect;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.vidal1shere.jasper.client.molang.MolangParser;
import com.vidal1shere.jasper.client.molang.MolangValue;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
 * Effect numbers that take a molang string. Decoding one while loading keeps a placeholder in range and collects the expression,
 * so EffectJson can decode the entry again with the values when it fires; those values are clamped instead of failing.
 */
@OnlyIn(Dist.CLIENT)
public final class EffectCodecs {
    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();

    public static Codec<Float> number(float min, float max) {
        return Codec.either(Codec.FLOAT, Codec.STRING).flatXmap(
                either -> either.map(value -> range(value, min, max), expression -> expression(expression, min, max)),
                value -> DataResult.success(Either.left(value)));
    }

    public static Codec<Integer> integer(int min, int max) {
        return number(min, max).flatXmap(value -> {
            Scope scope = SCOPE.get();
            if (value != Math.rint(value) && (scope == null || !scope.resolving)) {
                return DataResult.error(() -> "expected a whole number, got " + value);
            }
            return DataResult.success(Math.round(value));
        }, value -> DataResult.success(value.floatValue()));
    }

    public static Codec<Vec3> vector() {
        return number(-Float.MAX_VALUE, Float.MAX_VALUE).listOf(3, 3).xmap(
                list -> new Vec3(list.get(0), list.get(1), list.get(2)),
                vector -> List.of((float) vector.x, (float) vector.y, (float) vector.z));
    }

    public static <T> Collected<T> collect(Supplier<T> decode) {
        Scope scope = new Scope(false);
        SCOPE.set(scope);
        try {
            return new Collected<>(decode.get(), Map.copyOf(scope.expressions));
        } finally {
            SCOPE.remove();
        }
    }

    public static <T> T resolving(Supplier<T> decode) {
        SCOPE.set(new Scope(true));
        try {
            return decode.get();
        } finally {
            SCOPE.remove();
        }
    }

    private static DataResult<Float> range(float value, float min, float max) {
        Scope scope = SCOPE.get();
        if (scope != null && scope.resolving) {
            return DataResult.success(Mth.clamp(value, min, max));
        }
        if (value < min || value > max) {
            return DataResult.error(() -> "value " + value + " outside of range [" + min + ":" + max + "]");
        }
        return DataResult.success(value);
    }

    private static DataResult<Float> expression(String expression, float min, float max) {
        MolangValue value;
        try {
            value = MolangParser.parse(expression);
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "invalid molang '" + expression + "': " + e.getMessage());
        }
        if (value.isConstant()) {
            return range(value.get(null), min, max);
        }
        Scope scope = SCOPE.get();
        if (scope == null) {
            return DataResult.error(() -> "molang is only read in effect files: '" + expression + "'");
        }
        scope.expressions.put(expression, value);
        return DataResult.success(Mth.clamp(0.0F, min, max));
    }

    public record Collected<T>(T value, Map<String, MolangValue> expressions) {
    }

    private static final class Scope {
        private final boolean resolving;
        private final Map<String, MolangValue> expressions = new HashMap<>();

        private Scope(boolean resolving) {
            this.resolving = resolving;
        }
    }
}
