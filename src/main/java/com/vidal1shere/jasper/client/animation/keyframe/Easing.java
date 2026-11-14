package com.vidal1shere.jasper.client.animation.keyframe;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public enum Easing {
    LINEAR,
    STEP,
    CATMULLROM,
    EASE_IN_SINE,
    EASE_OUT_SINE,
    EASE_IN_OUT_SINE,
    EASE_IN_QUAD,
    EASE_OUT_QUAD,
    EASE_IN_OUT_QUAD,
    EASE_IN_CUBIC,
    EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC,
    EASE_IN_QUART,
    EASE_OUT_QUART,
    EASE_IN_OUT_QUART,
    EASE_IN_QUINT,
    EASE_OUT_QUINT,
    EASE_IN_OUT_QUINT,
    EASE_IN_EXPO,
    EASE_OUT_EXPO,
    EASE_IN_OUT_EXPO,
    EASE_IN_CIRC,
    EASE_OUT_CIRC,
    EASE_IN_OUT_CIRC,
    EASE_IN_BACK,
    EASE_OUT_BACK,
    EASE_IN_OUT_BACK,
    EASE_IN_ELASTIC,
    EASE_OUT_ELASTIC,
    EASE_IN_OUT_ELASTIC,
    EASE_IN_BOUNCE,
    EASE_OUT_BOUNCE,
    EASE_IN_OUT_BOUNCE;

    private static final Easing[] VALUES = values();

    private final String id = name().replace("_", "").toLowerCase(Locale.ROOT);

    @Nullable
    public static Easing byName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (key.equals("none")) {
            return LINEAR;
        }
        for (Easing easing : VALUES) {
            if (easing.id.equals(key)) {
                return easing;
            }
        }
        return null;
    }

    public static String names() {
        StringBuilder builder = new StringBuilder();
        for (Easing easing : VALUES) {
            builder.append(builder.isEmpty() ? "" : ", ").append(easing.id);
        }
        return builder.toString();
    }

    public boolean usesArgument() {
        return switch (this) {
            case EASE_IN_BACK, EASE_OUT_BACK, EASE_IN_OUT_BACK, EASE_IN_ELASTIC, EASE_OUT_ELASTIC, EASE_IN_OUT_ELASTIC,
                 EASE_IN_BOUNCE, EASE_OUT_BOUNCE, EASE_IN_OUT_BOUNCE -> true;
            default -> false;
        };
    }

    public float defaultArgument() {
        return switch (this) {
            case EASE_IN_BACK, EASE_OUT_BACK, EASE_IN_OUT_BACK -> 1.70158F;
            case EASE_IN_ELASTIC, EASE_OUT_ELASTIC, EASE_IN_OUT_ELASTIC -> 1.0F;
            case EASE_IN_BOUNCE, EASE_OUT_BOUNCE, EASE_IN_OUT_BOUNCE -> 0.5F;
            default -> 0.0F;
        };
    }

    public float apply(float t, float arg) {
        return switch (this) {
            case LINEAR, CATMULLROM -> t;
            case STEP -> t >= 1.0F ? 1.0F : 0.0F;
            case EASE_IN_SINE -> 1.0F - Mth.cos(t * Mth.HALF_PI);
            case EASE_OUT_SINE -> Mth.sin(t * Mth.HALF_PI);
            case EASE_IN_OUT_SINE -> -(Mth.cos(Mth.PI * t) - 1.0F) / 2.0F;
            case EASE_IN_QUAD -> in(t, 2);
            case EASE_OUT_QUAD -> out(t, 2);
            case EASE_IN_OUT_QUAD -> inOut(t, 2);
            case EASE_IN_CUBIC -> in(t, 3);
            case EASE_OUT_CUBIC -> out(t, 3);
            case EASE_IN_OUT_CUBIC -> inOut(t, 3);
            case EASE_IN_QUART -> in(t, 4);
            case EASE_OUT_QUART -> out(t, 4);
            case EASE_IN_OUT_QUART -> inOut(t, 4);
            case EASE_IN_QUINT -> in(t, 5);
            case EASE_OUT_QUINT -> out(t, 5);
            case EASE_IN_OUT_QUINT -> inOut(t, 5);
            case EASE_IN_EXPO -> t <= 0.0F ? 0.0F : (float) Math.pow(2.0, 10.0 * t - 10.0);
            case EASE_OUT_EXPO -> t >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
            case EASE_IN_OUT_EXPO -> t <= 0.0F || t >= 1.0F ? t
                    : t < 0.5F ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2.0F : (2.0F - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2.0F;
            case EASE_IN_CIRC -> 1.0F - Mth.sqrt(1.0F - t * t);
            case EASE_OUT_CIRC -> Mth.sqrt(1.0F - (t - 1.0F) * (t - 1.0F));
            case EASE_IN_OUT_CIRC -> t < 0.5F
                    ? (1.0F - Mth.sqrt(1.0F - 4.0F * t * t)) / 2.0F
                    : (Mth.sqrt(1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F)) + 1.0F) / 2.0F;
            case EASE_IN_BACK -> backIn(t, arg);
            case EASE_OUT_BACK -> 1.0F - backIn(1.0F - t, arg);
            case EASE_IN_OUT_BACK -> t < 0.5F ? backIn(2.0F * t, arg) / 2.0F : 1.0F - backIn(2.0F - 2.0F * t, arg) / 2.0F;
            case EASE_IN_ELASTIC -> 1.0F - elasticOut(1.0F - t, arg);
            case EASE_OUT_ELASTIC -> elasticOut(t, arg);
            case EASE_IN_OUT_ELASTIC -> t < 0.5F ? (1.0F - elasticOut(1.0F - 2.0F * t, arg)) / 2.0F : (1.0F + elasticOut(2.0F * t - 1.0F, arg)) / 2.0F;
            case EASE_IN_BOUNCE -> 1.0F - bounceOut(1.0F - t, arg);
            case EASE_OUT_BOUNCE -> bounceOut(t, arg);
            case EASE_IN_OUT_BOUNCE -> t < 0.5F ? (1.0F - bounceOut(1.0F - 2.0F * t, arg)) / 2.0F : (1.0F + bounceOut(2.0F * t - 1.0F, arg)) / 2.0F;
        };
    }

    private static float in(float t, int power) {
        return (float) Math.pow(t, power);
    }

    private static float out(float t, int power) {
        return 1.0F - (float) Math.pow(1.0F - t, power);
    }

    private static float inOut(float t, int power) {
        return t < 0.5F ? (float) Math.pow(2.0F * t, power) / 2.0F : 1.0F - (float) Math.pow(2.0F - 2.0F * t, power) / 2.0F;
    }

    private static float backIn(float t, float overshoot) {
        return t * t * ((overshoot + 1.0F) * t - overshoot);
    }

    private static float elasticOut(float t, float bounciness) {
        if (t <= 0.0F || t >= 1.0F) {
            return t;
        }
        float period = 0.3F / Math.max(bounciness, 0.001F);
        return (float) Math.pow(2.0, -10.0 * t) * Mth.sin((t - period / 4.0F) * Mth.TWO_PI / period) + 1.0F;
    }

    private static float bounceOut(float t, float bounciness) {
        float damping = Mth.clamp(bounciness, 0.0F, 1.0F) + 0.5F;
        float value;
        if (t < 1.0F / 2.75F) {
            value = 7.5625F * t * t;
            return value;
        } else if (t < 2.0F / 2.75F) {
            t -= 1.5F / 2.75F;
            value = 1.0F - (1.0F - (7.5625F * t * t + 0.75F)) * damping;
        } else if (t < 2.5F / 2.75F) {
            t -= 2.25F / 2.75F;
            value = 1.0F - (1.0F - (7.5625F * t * t + 0.9375F)) * damping;
        } else {
            t -= 2.625F / 2.75F;
            value = 1.0F - (1.0F - (7.5625F * t * t + 0.984375F)) * damping;
        }
        return value;
    }
}
