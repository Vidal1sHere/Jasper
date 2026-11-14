package com.vidal1shere.jasper.registry;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.attachment.AnimationData;
import com.vidal1shere.jasper.attachment.EntityOverlayData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class JasperAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Jasper.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AnimationData>> ANIMATIONS = TYPES.register("animations", () -> AttachmentType.builder(AnimationData::new).build());
    // Saved, unlike the rest, since an entity overlay can last indefinitely; it plays again when the entity loads
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EntityOverlayData>> ENTITY_OVERLAY = TYPES.register("entity_overlay", () -> AttachmentType.builder(() -> new EntityOverlayData(ResourceLocation.withDefaultNamespace("empty"), 1.0F)).serialize(EntityOverlayData.CODEC).build());
}
