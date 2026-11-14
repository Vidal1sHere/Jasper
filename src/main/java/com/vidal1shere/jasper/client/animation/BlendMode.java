package com.vidal1shere.jasper.client.animation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public enum BlendMode {
    REPLACE,
    ADDITIVE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static BlendMode byName(String name) {
        for (BlendMode mode : values()) {
            if (mode.id().equals(name)) {
                return mode;
            }
        }
        return null;
    }
}
