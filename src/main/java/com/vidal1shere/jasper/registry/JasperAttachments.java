package com.vidal1shere.jasper.registry;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.attachment.AnimationData;
import com.vidal1shere.jasper.attachment.SavedAnimationData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class JasperAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Jasper.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AnimationData>> ANIMATIONS = TYPES.register("animations", () -> AttachmentType.builder(AnimationData::new).build());
    // The animations of persistent layers, saved and kept through death; they play again when the entity loads
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SavedAnimationData>> SAVED_ANIMATIONS = TYPES.register("saved_animations", () -> AttachmentType.builder(() -> SavedAnimationData.EMPTY).serialize(SavedAnimationData.CODEC).copyOnDeath().build());
}
