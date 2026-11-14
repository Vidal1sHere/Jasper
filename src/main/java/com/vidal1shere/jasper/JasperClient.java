package com.vidal1shere.jasper;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.test.TestEntitiesClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value = Jasper.MOD_ID, dist = Dist.CLIENT)
public class JasperClient {
    public JasperClient(IEventBus modBus) {
        ClientAttachments.TYPES.register(modBus);
        if (!FMLEnvironment.production) {
            TestEntitiesClient.register(modBus);
        }
    }
}
