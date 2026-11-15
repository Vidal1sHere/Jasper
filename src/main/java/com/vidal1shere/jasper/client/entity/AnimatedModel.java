package com.vidal1shere.jasper.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vidal1shere.jasper.client.model.ModelParts;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.List;

// Subclasses call super.setupAnim first and add their own logic after it
@OnlyIn(Dist.CLIENT)
public abstract class AnimatedModel<T extends LivingEntity & AnimatedEntity> extends HierarchicalModel<T> {
    private final ModelParts parts;

    protected AnimatedModel(ModelPart root) {
        this.parts = new ModelParts(root, getClass().getSimpleName());
    }

    @Override
    public ModelPart root() {
        return parts.root();
    }

    public abstract ModelPart head();

    public ModelParts parts() {
        return parts;
    }

    @Nullable
    public ModelPart part(String bone) {
        return parts.part(bone);
    }

    @Nullable
    public Matrix4f matrix(Matrix4fc base, String bone) {
        return parts.matrix(base, bone);
    }

    @Nullable
    public List<ModelPart> chain(String bone) {
        return parts.chain(bone);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        EntityAdapter.captureLocators(this, poseStack);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        EntityAdapter.apply(entity, this, ageInTicks, netHeadYaw, headPitch);
    }
}
