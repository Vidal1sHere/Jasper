package com.vidal1shere.jasper;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

@Mod(Jasper.MOD_ID)
public class Jasper {
    public static final String MOD_ID = "jasper";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
