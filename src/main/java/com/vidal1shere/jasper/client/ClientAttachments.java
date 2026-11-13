package com.vidal1shere.jasper.client;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.attachment.ItemAnimationData;
import com.vidal1shere.jasper.client.attachment.PlayerAnimationData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

// Registered only on the client, which works because the attachment type registry is not synced; the state goes away with the client entity
public final class ClientAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Jasper.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemAnimationData>> ITEM_ANIMATIONS = TYPES.register("item_animations", () -> AttachmentType.builder(ItemAnimationData::new).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerAnimationData>> PLAYER_ANIMATIONS = TYPES.register("player_animations", () -> AttachmentType.builder(PlayerAnimationData::new).build());

    // The data of the entity a payload names, created when missing; null when this client does not know the entity
    @Nullable
    public static <T> T of(int entityId, Supplier<AttachmentType<T>> type) {
        Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(entityId);
        return entity == null ? null : entity.getData(type);
    }
}
