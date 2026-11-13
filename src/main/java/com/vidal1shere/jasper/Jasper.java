package com.vidal1shere.jasper;

import com.vidal1shere.jasper.network.JasperNetwork;
import com.vidal1shere.jasper.registry.JasperAttachments;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Jasper.MOD_ID)
public class Jasper {
    public static final String MOD_ID = "jasper";

    public Jasper(IEventBus modBus) {
        JasperAttachments.TYPES.register(modBus);
        modBus.addListener(JasperNetwork::onRegisterPayloads);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
