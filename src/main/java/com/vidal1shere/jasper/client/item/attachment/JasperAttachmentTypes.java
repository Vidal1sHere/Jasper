package com.vidal1shere.jasper.client.item.attachment;

import com.mojang.serialization.MapCodec;
import com.vidal1shere.jasper.Jasper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// The kinds the "type" of a jasper:animated attachment names; filled in the client mod constructor, before the first resource load
@OnlyIn(Dist.CLIENT)
public final class JasperAttachmentTypes {
    private static final Map<ResourceLocation, MapCodec<? extends JasperModelAttachment>> TYPES = new ConcurrentHashMap<>();

    public static final ResourceLocation ITEM_MODEL = register(Jasper.id("item_model"), ItemModelAttachment.CODEC);
    public static final ResourceLocation ITEM = register(Jasper.id("item"), ItemStackAttachment.CODEC);
    public static final ResourceLocation PLAYER_ARM = register(Jasper.id("player_arm"), PlayerArmAttachment.CODEC);

    public static ResourceLocation register(ResourceLocation id, MapCodec<? extends JasperModelAttachment> codec) {
        if (TYPES.putIfAbsent(id, codec) != null) {
            throw new IllegalStateException("Attachment type " + id + " is already registered");
        }
        return id;
    }

    @Nullable
    public static MapCodec<? extends JasperModelAttachment> get(ResourceLocation id) {
        return TYPES.get(id);
    }
}
