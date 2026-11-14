package com.vidal1shere.jasper;

import com.vidal1shere.jasper.network.JasperNetwork;
import com.vidal1shere.jasper.registry.JasperAttachments;
import com.vidal1shere.jasper.test.TestEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Jasper.MOD_ID)
public class Jasper {
    public static final String MOD_ID = "jasper";

    public Jasper(IEventBus modBus) {
        JasperAttachments.TYPES.register(modBus);
        modBus.addListener(JasperNetwork::onRegisterPayloads);
        if (!FMLEnvironment.production) {
            TestEntities.register(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
