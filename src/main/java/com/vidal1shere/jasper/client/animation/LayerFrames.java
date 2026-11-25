package com.vidal1shere.jasper.client.animation;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.locator.Locator;
import com.vidal1shere.jasper.client.attachment.ClientAnimationData;
import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.client.molang.BuiltinQueries;
import com.vidal1shere.jasper.client.molang.MolangContext;
import com.vidal1shere.jasper.client.molang.QuerySource;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

// Each adapter keeps its own, since their frames live until different render stages
@OnlyIn(Dist.CLIENT)
public final class LayerFrames {
    private static final Set<String> BONES = new HashSet<>();

    private final MolangContext context = new MolangContext();
    private final List<TrackFrame> frames = new ArrayList<>();
    private final List<Layer> layers = new ArrayList<>();
    @Nullable
    private final String[][] mirrorPairs;
    @Nullable
    private final QuerySource queries;
    private int size;

    public LayerFrames(@Nullable String[][] mirrorPairs, @Nullable QuerySource queries) {
        this.mirrorPairs = mirrorPairs;
        this.queries = queries;
    }

    public int evaluate(LivingEntity entity, AnimationTarget target, @Nullable InteractionHand hand, float partialTick) {
        size = 0;
        layers.clear();
        ClientAnimationData data = entity.getExistingDataOrNull(ClientAttachments.ANIMATIONS);
        if (data == null) {
            return 0;
        }
        double now = entity.level().getGameTime() + partialTick;
        InteractionHand filled = null;
        for (Layer layer : JasperLayers.sorted()) {
            LayerState track = data.existing(target, layer);
            if (track == null) {
                continue;
            }
            track.track().update(now);
            if (!track.track().isActive()) {
                continue;
            }
            InteractionHand layerHand = hand == null ? track.hand() : hand;
            if (layerHand != filled) {
                BuiltinQueries.fill(context, entity, layerHand, partialTick);
                if (queries != null) {
                    queries.fill(context, entity, layerHand, partialTick);
                }
                filled = layerHand;
            }
            if (frames.size() <= size) {
                frames.add(new TrackFrame());
            }
            boolean mirrored = mirrorPairs != null && BuiltinQueries.isLeftArm(entity, layerHand);
            frames.get(size++).evaluate(track.track(), now, context, mirrored ? mirrorPairs : null);
            layers.add(layer);
        }
        return size;
    }

    public int size() {
        return size;
    }

    public TrackFrame frame(int index) {
        return frames.get(index);
    }

    public List<TrackFrame> frames() {
        return frames;
    }

    public boolean contains(Layer layer) {
        return layers.contains(layer);
    }

    public void apply(Function<String, ModelPart> parts, Set<String> ignored) {
        for (int f = 0; f < size; f++) {
            TrackFrame frame = frames.get(f);
            BONES.clear();
            for (int i = 0; i < frame.size(); i++) {
                JasperAnimation animation = frame.animation(i);
                if (animation == null) {
                    continue;
                }
                for (String bone : animation.keyframes().bones().keySet()) {
                    Locator locator = animation.locators().get(bone);
                    if ((locator == null || !locator.animated()) && !ignored.contains(bone)) {
                        BONES.add(bone);
                    }
                }
            }
            for (String bone : BONES) {
                ModelPart part = parts.apply(bone);
                if (part != null) {
                    frame.apply(bone, part, null);
                }
            }
        }
    }
}
