package com.vidal1shere.jasper.client.item.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

// Something drawn at a bone of a 3D item, a bone of its own; render runs at its place, in item space with Y up
@OnlyIn(Dist.CLIENT)
public abstract class JasperModelAttachment {
    private final AttachmentPlacement placement;
    // Holds no cubes, only the pose item animations write to the bone of this attachment
    private final ModelPart pose = new ModelPart(List.of(), Map.of());

    protected JasperModelAttachment(AttachmentPlacement placement) {
        this.placement = placement;
    }

    public AttachmentPlacement placement() {
        return placement;
    }

    public ModelPart pose() {
        return pose;
    }

    public String name() {
        return placement.name();
    }

    // Called while the item model loads, for kinds that draw baked models
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
    }

    public void bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter) {
    }

    public abstract void render(AttachmentContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay);

    // The texture of what render draws, for afterimages that copy the texture; null leaves it out of them
    @Nullable
    public ResourceLocation texture(AttachmentContext context) {
        return null;
    }
}
