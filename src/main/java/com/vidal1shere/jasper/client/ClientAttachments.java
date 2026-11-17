package com.vidal1shere.jasper.client;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.EffectData;
import com.vidal1shere.jasper.client.attachment.LocatorData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// Registered only on the client, which works because the attachment type registry is not synced; the state goes away with the client entity
@OnlyIn(Dist.CLIENT)
public final class ClientAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Jasper.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ClientAnimationData>> ANIMATIONS = TYPES.register("client_animations", () -> AttachmentType.builder(ClientAnimationData::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LocatorData>> LOCATORS = TYPES.register("locators", () -> AttachmentType.builder(LocatorData::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EffectData>> EFFECTS = TYPES.register("effects", () -> AttachmentType.builder(EffectData::new).build());
}
