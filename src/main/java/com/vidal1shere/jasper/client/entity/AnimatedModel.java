package com.vidal1shere.jasper.client.entity;

import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
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

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        ClientEntityAnimations.apply(entity, this, ageInTicks, netHeadYaw, headPitch);
    }
}
