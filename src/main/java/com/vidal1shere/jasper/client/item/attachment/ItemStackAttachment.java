package com.vidal1shere.jasper.client.item.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// jasper:item, a stack drawn with its own item model; override stack to show what the item holds, like the ammo it is loaded with
@OnlyIn(Dist.CLIENT)
public class ItemStackAttachment extends JasperModelAttachment {
    public static final MapCodec<ItemStackAttachment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentPlacement.CODEC.forGetter(JasperModelAttachment::placement),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(attachment -> attachment.stack.getItem())
    ).apply(instance, (placement, item) -> new ItemStackAttachment(placement, new ItemStack(item))));

    private final ItemStack stack;

    public ItemStackAttachment(AttachmentPlacement placement, ItemStack stack) {
        super(placement);
        this.stack = stack;
    }

    protected ItemStack stack(AttachmentContext context) {
        return stack;
    }

    // Item models draw from the block atlas; a stack with a renderer of its own may use other textures
    @Override
    public ResourceLocation texture(AttachmentContext context) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public void render(AttachmentContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack shown = stack(context);
        // A 3D item holding a stack of its own item would draw itself forever
        if (shown.isEmpty() || shown.getItem() == context.stack().getItem()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(context.holder(), shown, ItemDisplayContext.NONE, false, poseStack, buffer, minecraft.level, packedLight, packedOverlay, 0);
    }
}
