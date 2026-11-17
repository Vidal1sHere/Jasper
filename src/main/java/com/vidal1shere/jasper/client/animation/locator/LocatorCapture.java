package com.vidal1shere.jasper.client.animation.locator;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.animation.JasperAnimation;
import com.vidal1shere.jasper.client.animation.TrackFrame;
import com.vidal1shere.jasper.client.animation.pose.BonePose;
import com.vidal1shere.jasper.client.attachment.LocatorData;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.util.ClientView;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

// Positions are taken from the matrices a render pass is drawing with, which are relative to the camera in world axes, the first person hands included
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class LocatorCapture {
    private static final Set<String> CAPTURED = new HashSet<>();
    private static final BonePose POSE = new BonePose();
    private static final Vector3f POINT = new Vector3f();

    // Entities also render outside of the world, like the player in the inventory screen
    private static boolean inLevel;
    private static long renderFrame;

    public static long renderFrame() {
        return renderFrame;
    }

    public static boolean inLevel() {
        return inLevel;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            inLevel = true;
            renderFrame++;
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            inLevel = false;
        }
    }

    public static void capture(Entity entity, AnimationTarget target, List<TrackFrame> frames, int count, Function<String, Matrix4fc> parents, UnaryOperator<String> counterpart, float unit) {
        if (target == AnimationTarget.MODEL && !inLevel) {
            return;
        }
        CAPTURED.clear();
        LocatorData data = null;
        Vec3 camera = ClientView.camera();
        for (int f = 0; f < count; f++) {
            TrackFrame frame = frames.get(f);
            for (int i = 0; i < frame.size(); i++) {
                JasperAnimation animation = frame.animation(i);
                if (animation == null) {
                    continue;
                }
                for (String name : animation.locators().keySet()) {
                    if (CAPTURED.contains(name)) {
                        continue;
                    }
                    Matrix4f matrix = matrix(name, animation.locators(), frame.mirrored(), frames, count, parents, counterpart, unit);
                    if (matrix == null) {
                        continue;
                    }
                    CAPTURED.add(name);
                    matrix.transformPosition(POINT.zero());
                    if (data == null) {
                        data = entity.getData(ClientAttachments.LOCATORS);
                    }
                    data.put(target, name, camera.add(POINT.x, POINT.y, POINT.z), entity.level().getGameTime(), renderFrame);
                }
            }
        }
    }

    public static Matrix4f part(Matrix4fc base, ModelPart part) {
        return new Matrix4f(base).translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F).rotateZYX(part.zRot, part.yRot, part.xRot).scale(part.xScale, part.yScale, part.zScale);
    }

    @Nullable
    private static Matrix4f matrix(String name, Map<String, Locator> locators, boolean mirrored, List<TrackFrame> frames, int count, Function<String, Matrix4fc> parents, UnaryOperator<String> counterpart, float unit) {
        Locator locator = locators.get(name);
        String parent = mirrored ? counterpart.apply(locator.parent()) : locator.parent();
        Matrix4f matrix;
        if (!locator.parent().equals(name) && locators.containsKey(locator.parent())) {
            matrix = matrix(locator.parent(), locators, mirrored, frames, count, parents, counterpart, unit);
        } else {
            Matrix4fc base = parents.apply(parent);
            matrix = base == null ? null : new Matrix4f(base);
        }
        if (matrix == null) {
            return null;
        }
        PartPose rest = locator.rest(mirrored);
        POSE.reset();
        POSE.position.set(rest.x, rest.y, rest.z);
        if (locator.animated()) {
            for (int f = 0; f < count; f++) {
                frames.get(f).resolve(name, POSE, rest);
            }
        }
        return matrix.translate(POSE.position.x * unit, POSE.position.y * unit, POSE.position.z * unit)
                .rotateZYX(POSE.rotation.z, POSE.rotation.y, POSE.rotation.x)
                .scale(POSE.scale.x, POSE.scale.y, POSE.scale.z);
    }
}
