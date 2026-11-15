package com.vidal1shere.jasper.client.item.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vidal1shere.jasper.client.animation.pose.PartLookup;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnimatedItemModel extends Model {
    private final PartLookup parts;

    public AnimatedItemModel(ModelPart root, String name) {
        super(RenderType::entityCutoutNoCull);
        this.parts = new PartLookup(root, name);
    }

    public PartLookup parts() {
        return parts;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        parts.root().render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
