package com.vidal1shere.jasper.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Subclasses call super.setupAnim first and add their own logic after it
@OnlyIn(Dist.CLIENT)
public abstract class AnimatedModel<T extends LivingEntity & AnimatedEntity> extends HierarchicalModel<T> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ModelPart root;
    private final Map<String, Optional<ModelPart>> parts = new HashMap<>();
    private final Set<String> missing = new HashSet<>();
    private final Map<String, Optional<List<ModelPart>>> chains = new HashMap<>();

    protected AnimatedModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    public abstract ModelPart head();

    // Unlike getAnyDescendantWithName, "root" is the part named root, not the root of the mesh, which models often do not render
    @Nullable
    public ModelPart part(String bone) {
        Optional<ModelPart> part = parts.computeIfAbsent(bone, name -> root.getAllParts().filter(parent -> parent.hasChild(name)).findFirst().map(parent -> parent.getChild(name)));
        if (part.isEmpty() && missing.add(bone)) {
            LOGGER.warn("{} has no part named '{}', its animations skip it", getClass().getSimpleName(), bone);
        }
        return part.orElse(null);
    }

    // The matrix a part renders with, from the matrix the model renders with
    @Nullable
    public Matrix4f matrix(Matrix4fc base, String bone) {
        List<ModelPart> chain = chains.computeIfAbsent(bone, this::chain).orElse(null);
        if (chain == null) {
            return null;
        }
        Matrix4f matrix = new Matrix4f(base);
        for (ModelPart part : chain) {
            matrix = LocatorCapture.part(matrix, part);
        }
        return matrix;
    }

    // ModelPart keeps no parent, so the ancestors are the parts that contain it, the biggest first
    private Optional<List<ModelPart>> chain(String bone) {
        ModelPart part = part(bone);
        if (part == null) {
            return Optional.empty();
        }
        List<ModelPart> chain = new ArrayList<>(root.getAllParts()
                .filter(ancestor -> ancestor != part && ancestor.getAllParts().anyMatch(child -> child == part))
                .sorted(Comparator.comparingLong(ancestor -> -ancestor.getAllParts().count()))
                .toList());
        chain.add(part);
        return Optional.of(chain);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        EntityAdapter.captureLocators(this, poseStack);
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        EntityAdapter.apply(entity, this, ageInTicks, netHeadYaw, headPitch);
    }
}
