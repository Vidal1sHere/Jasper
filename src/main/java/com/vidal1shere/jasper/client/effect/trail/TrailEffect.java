package com.vidal1shere.jasper.client.effect.trail;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vidal1shere.jasper.client.animation.keyframe.Easing;
import com.vidal1shere.jasper.client.effect.AnimationEffect;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Optional;

// The locators are the ordered section of one ribbon, or with one locator the path of a camera facing ribbon of width blocks; lifetime and interval are game ticks; colors and alphas take one value or one per locator, the last repeating, and go from their start to their end over the life of a ring
@OnlyIn(Dist.CLIENT)
public record TrailEffect(List<String> locators, float lifetime, float interval, List<TextColor> color, Optional<List<TextColor>> endColor,
                          List<Float> alpha, List<Float> endAlpha, Easing easing, float shrink, Blend blend, Light light,
                          Optional<ResourceLocation> texture, float width, int smoothing, Space space) implements AnimationEffect {
    private static final Codec<Float> UNIT = Codec.floatRange(0.0F, 1.0F);
    private static final Codec<Float> POSITIVE = Codec.floatRange(0.001F, Float.MAX_VALUE);

    public static final MapCodec<TrailEffect> CODEC = RecordCodecBuilder.<TrailEffect>mapCodec(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("locators").forGetter(TrailEffect::locators),
            POSITIVE.optionalFieldOf("lifetime", 10.0F).forGetter(TrailEffect::lifetime),
            POSITIVE.optionalFieldOf("interval", 0.5F).forGetter(TrailEffect::interval),
            oneOrMore(TextColor.CODEC).optionalFieldOf("color", List.of(TextColor.fromLegacyFormat(ChatFormatting.WHITE))).forGetter(TrailEffect::color),
            oneOrMore(TextColor.CODEC).optionalFieldOf("end_color").forGetter(TrailEffect::endColor),
            oneOrMore(UNIT).optionalFieldOf("alpha", List.of(1.0F)).forGetter(TrailEffect::alpha),
            oneOrMore(UNIT).optionalFieldOf("end_alpha", List.of(0.0F)).forGetter(TrailEffect::endAlpha),
            Easing.CODEC.optionalFieldOf("easing", Easing.LINEAR).forGetter(TrailEffect::easing),
            UNIT.optionalFieldOf("shrink", 0.0F).forGetter(TrailEffect::shrink),
            Blend.CODEC.optionalFieldOf("blend", Blend.ADDITIVE).forGetter(TrailEffect::blend),
            Light.CODEC.optionalFieldOf("light", Light.FULL).forGetter(TrailEffect::light),
            ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(TrailEffect::texture),
            POSITIVE.optionalFieldOf("width", 0.25F).forGetter(TrailEffect::width),
            Codec.intRange(0, 16).optionalFieldOf("smoothing", 3).forGetter(TrailEffect::smoothing),
            Space.CODEC.optionalFieldOf("space", Space.WORLD).forGetter(TrailEffect::space)
    ).apply(instance, TrailEffect::new)).validate(effect -> effect.locators.isEmpty()
            ? DataResult.error(() -> "a trail needs at least one locator")
            : DataResult.success(effect));

    @Override
    public PlayingEffect start(EffectContext context) {
        return Trails.start(this, context);
    }

    // Age is 0 when a ring is born and 1 when it dies; index is its point in the section
    int argb(float age, int index) {
        float t = easing.apply(Mth.clamp(age, 0.0F, 1.0F), easing.defaultArgument());
        int start = at(color, index).getValue();
        int end = at(endColor.orElse(color), index).getValue();
        int rgb = FastColor.ARGB32.lerp(t, start, end) & 0xFFFFFF;
        float opacity = Mth.lerp(t, at(alpha, index), at(endAlpha, index));
        return Math.round(Mth.clamp(opacity, 0.0F, 1.0F) * 255.0F) << 24 | rgb;
    }

    float shrinking(float age) {
        return shrink * easing.apply(Mth.clamp(age, 0.0F, 1.0F), easing.defaultArgument());
    }

    private static <T> T at(List<T> values, int index) {
        return values.get(Math.min(index, values.size() - 1));
    }

    private static <T> Codec<List<T>> oneOrMore(Codec<T> codec) {
        return Codec.either(codec, ExtraCodecs.nonEmptyList(codec.listOf())).xmap(
                either -> either.map(List::of, list -> list),
                list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list)
        );
    }

    public enum Blend implements StringRepresentable {
        ADDITIVE("additive"),
        TRANSLUCENT("translucent");

        public static final Codec<Blend> CODEC = StringRepresentable.fromEnum(Blend::values);

        private final String name;

        Blend(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Light implements StringRepresentable {
        FULL("full"),
        WORLD("world");

        public static final Codec<Light> CODEC = StringRepresentable.fromEnum(Light::values);

        private final String name;

        Light(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    // Entity rings move with the entity position, not its rotation
    public enum Space implements StringRepresentable {
        WORLD("world"),
        ENTITY("entity");

        public static final Codec<Space> CODEC = StringRepresentable.fromEnum(Space::values);

        private final String name;

        Space(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
