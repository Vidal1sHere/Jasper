package com.vidal1shere.jasper;

import com.vidal1shere.jasper.client.ClientAttachments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = Jasper.MOD_ID, dist = Dist.CLIENT)
public class JasperClient {
    public JasperClient(IEventBus modBus) {
        ClientAttachments.TYPES.register(modBus);
    }
}
