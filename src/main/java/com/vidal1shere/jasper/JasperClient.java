package com.vidal1shere.jasper;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.JasperClientConfig;
import com.vidal1shere.jasper.test.CinderDetonatorModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = Jasper.MOD_ID, dist = Dist.CLIENT)
public class JasperClient {
    public JasperClient(IEventBus modBus, ModContainer container) {
        ClientAttachments.TYPES.register(modBus);
        container.registerConfig(ModConfig.Type.CLIENT, JasperClientConfig.SPEC, "jasper-client.toml");
        if (!FMLEnvironment.production) {
            modBus.addListener(CinderDetonatorModel::onRegisterLayerDefinitions);
        }
    }
}
