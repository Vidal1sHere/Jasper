package com.vidal1shere.jasper.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class JasperClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FLASH = BUILDER
            .comment("Lets animation effects flash the screen with a color.")
            .define("effects.flash", true);

    public static final ModConfigSpec.BooleanValue FOV = BUILDER
            .comment("Lets animation effects change the field of view. The vanilla FOV Effects option also scales it.")
            .define("effects.fov", true);

    public static final ModConfigSpec.DoubleValue HIT_STOP_SCALE = BUILDER
            .comment("Multiplies how long animation effects freeze the game. 0 turns hit stop off.")
            .defineInRange("effects.hit_stop_scale", 1.0, 0.0, 2.0);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
