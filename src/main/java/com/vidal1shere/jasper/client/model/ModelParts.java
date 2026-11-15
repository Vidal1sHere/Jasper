package com.vidal1shere.jasper.client.model;

import com.mojang.logging.LogUtils;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import net.minecraft.client.model.geom.ModelPart;
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

@OnlyIn(Dist.CLIENT)
public final class ModelParts {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> BONES = new HashSet<>();

    private final ModelPart root;
    private final String owner;
    private final Map<String, Optional<ModelPart>> parts = new HashMap<>();
    private final Set<String> missing = new HashSet<>();
    private final Map<String, Optional<List<ModelPart>>> chains = new HashMap<>();

    public ModelParts(ModelPart root, String owner) {
        this.root = root;
        this.owner = owner;
    }

    public ModelPart root() {
        return root;
    }

    // Every part the frame animates, except locators and the ignored bones, which are not parts
    public void apply(TrackFrame frame, Set<String> ignored) {
        BONES.clear();
        for (int i = 0; i < frame.size(); i++) {
            JasperAnimation animation = frame.animation(i);
            if (animation == null) {
                continue;
            }
            for (String bone : animation.keyframes().bones().keySet()) {
                if (!animation.locators().containsKey(bone) && !ignored.contains(bone)) {
                    BONES.add(bone);
                }
            }
        }
        for (String bone : BONES) {
            ModelPart part = part(bone);
            if (part != null) {
                frame.apply(bone, part, null);
            }
        }
    }

    // Unlike getAnyDescendantWithName, "root" is the part named root, not the root of the mesh, which models often do not render
    @Nullable
    public ModelPart part(String bone) {
        Optional<ModelPart> part = parts.computeIfAbsent(bone, name -> root.getAllParts().filter(parent -> parent.hasChild(name)).findFirst().map(parent -> parent.getChild(name)));
        if (part.isEmpty() && missing.add(bone)) {
            LOGGER.warn("{} has no part named '{}', its animations skip it", owner, bone);
        }
        return part.orElse(null);
    }

    // The matrix a part renders with, from the matrix the model renders with
    @Nullable
    public Matrix4f matrix(Matrix4fc base, String bone) {
        List<ModelPart> chain = chain(bone);
        if (chain == null) {
            return null;
        }
        Matrix4f matrix = new Matrix4f(base);
        for (ModelPart part : chain) {
            matrix = LocatorCapture.part(matrix, part);
        }
        return matrix;
    }

    // The part and its ancestors, the biggest first
    @Nullable
    public List<ModelPart> chain(String bone) {
        return chains.computeIfAbsent(bone, this::findChain).orElse(null);
    }

    // ModelPart keeps no parent, so the ancestors are the parts that contain it
    private Optional<List<ModelPart>> findChain(String bone) {
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
}
