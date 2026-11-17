package com.vidal1shere.jasper.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// Data, saves and commands write an enum constant as its lowercase name
public final class EnumNames {
    public static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static <E extends Enum<E>> E byName(Class<E> type, String name) {
        for (E value : type.getEnumConstants()) {
            if (name(value).equals(name)) {
                return value;
            }
        }
        return null;
    }

    public static List<String> names(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants()).map(EnumNames::name).toList();
    }

    public static <E extends Enum<E>> Codec<E> codec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            E value = byName(type, name);
            return value == null ? DataResult.error(() -> "unknown value '" + name + "', expected one of " + names(type)) : DataResult.success(value);
        }, EnumNames::name);
    }
}
