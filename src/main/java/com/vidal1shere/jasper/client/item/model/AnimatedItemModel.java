package com.vidal1shere.jasper.client.item.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vidal1shere.jasper.client.animation.pose.PartLookup;
import com.vidal1shere.jasper.client.item.attachment.JasperModelAttachment;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class AnimatedItemModel extends Model {
    private final PartLookup parts;
    private final List<JasperModelAttachment> attachments = new ArrayList<>();

    public AnimatedItemModel(ModelPart root, String name) {
        super(RenderType::entityCutoutNoCull);
        this.parts = new PartLookup(root, name);
    }

    public PartLookup parts() {
        return parts;
    }

    // Drawn with the attachments of the item JSON, which come first
    public List<JasperModelAttachment> attachments() {
        return Collections.unmodifiableList(attachments);
    }

    protected void attach(JasperModelAttachment attachment) {
        if (attachments.stream().anyMatch(other -> other.name().equals(attachment.name()))) {
            throw new IllegalArgumentException(parts.owner() + " already has an attachment named " + attachment.name());
        }
        attachments.add(attachment);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        parts.root().render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
