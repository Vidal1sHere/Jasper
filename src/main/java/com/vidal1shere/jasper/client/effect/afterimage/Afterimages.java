package com.vidal1shere.jasper.client.effect.afterimage;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.spec.Blend;
import com.vidal1shere.jasper.client.effect.spec.Light;
import com.vidal1shere.jasper.client.entity.AnimatedModel;
import com.vidal1shere.jasper.client.item.model.AnimatedItemModel;
import com.vidal1shere.jasper.client.player.PlayerBones;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.util.ClientView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// A copy is the mesh the model drew in that frame, since the model is shared and posed again for every render
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class Afterimages {
    private static final RenderType ADDITIVE = renderType("jasper_afterimage_additive", Blend.ADDITIVE);
    private static final RenderType TRANSLUCENT = renderType("jasper_afterimage_translucent", Blend.TRANSLUCENT);

    private static final List<PlayingAfterimage> ACTIVE = new ArrayList<>();
    private static final List<Copy> COPIES = new ArrayList<>();
    private static final Recorder RECORDER = new Recorder();

    static PlayingAfterimage start(AfterimageEffect effect, EffectContext context) {
        PlayingAfterimage afterimage = new PlayingAfterimage(effect, context);
        ACTIVE.add(afterimage);
        return afterimage;
    }

    static void remove(PlayingAfterimage afterimage) {
        ACTIVE.remove(afterimage);
        COPIES.removeIf(copy -> copy.owner == afterimage);
        afterimage.copies = 0;
    }

    public static boolean wants(LivingEntity entity, AnimationTarget target, boolean hand) {
        if (ACTIVE.isEmpty() || !hand && !LocatorCapture.inLevel()) {
            return false;
        }
        double now = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        for (PlayingAfterimage afterimage : ACTIVE) {
            if (matches(afterimage, entity, target, hand) && afterimage.due(now)) {
                return true;
            }
        }
        return false;
    }

    public static void capture(LivingEntity entity, EntityModel<?> model, PoseStack poseStack, int packedLight, ResourceLocation texture) {
        capture(entity, AnimationTarget.MODEL, false, packedLight, texture, effect -> {
            if (effect.bones().isEmpty()) {
                model.renderToBuffer(poseStack, RECORDER, packedLight, OverlayTexture.NO_OVERLAY, -1);
            } else {
                for (String bone : effect.bones()) {
                    renderBone(model, bone, poseStack, packedLight);
                }
            }
        });
    }

    public static void captureItem(LivingEntity holder, AnimationTarget target, boolean hand, AnimatedItemModel model, PoseStack poseStack, int packedLight, ResourceLocation texture) {
        capture(holder, target, hand, packedLight, texture, effect -> {
            if (effect.bones().isEmpty()) {
                model.renderToBuffer(poseStack, RECORDER, packedLight, OverlayTexture.NO_OVERLAY, -1);
            } else {
                for (String bone : effect.bones()) {
                    renderChain(model.parts().chain(bone), poseStack, packedLight);
                }
            }
        });
    }

    private static void capture(LivingEntity entity, AnimationTarget target, boolean hand, int packedLight, ResourceLocation texture, Consumer<AfterimageEffect> record) {
        double now = entity.level().getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 camera = ClientView.camera();
        for (PlayingAfterimage afterimage : ACTIVE) {
            if (!matches(afterimage, entity, target, hand) || !afterimage.due(now)) {
                continue;
            }
            AfterimageEffect effect = afterimage.effect;
            RECORDER.start();
            record.accept(effect);
            int light = effect.light() == Light.FULL ? LightTexture.FULL_BRIGHT : packedLight;
            COPIES.add(new Copy(afterimage, RECORDER.finish(), camera, effect.texture() ? texture : null, light, now));
            afterimage.copied(now);
        }
    }

    private static boolean matches(PlayingAfterimage afterimage, LivingEntity entity, AnimationTarget target, boolean hand) {
        return afterimage.entity == entity && afterimage.target == target && afterimage.hand == hand;
    }

    private static void renderBone(EntityModel<?> model, String bone, PoseStack poseStack, int packedLight) {
        if (model instanceof PlayerModel<?> player) {
            for (String part : bone.equals(PlayerBones.ROOT) ? PlayerBones.PARTS : List.of(bone)) {
                renderPlayerPart(PlayerBones.part(player, part), poseStack, packedLight);
                renderPlayerPart(PlayerBones.overlay(player, part), poseStack, packedLight);
            }
        } else if (model instanceof AnimatedModel<?> animated) {
            renderChain(animated.chain(bone), poseStack, packedLight);
        }
    }

    private static void renderChain(@Nullable List<ModelPart> chain, PoseStack poseStack, int packedLight) {
        if (chain == null) {
            return;
        }
        poseStack.pushPose();
        for (int i = 0; i < chain.size() - 1; i++) {
            chain.get(i).translateAndRotate(poseStack);
        }
        chain.getLast().render(poseStack, RECORDER, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static void renderPlayerPart(@Nullable ModelPart part, PoseStack poseStack, int packedLight) {
        if (part != null) {
            part.render(poseStack, RECORDER, packedLight, OverlayTexture.NO_OVERLAY);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || level == null) {
            return;
        }
        ACTIVE.removeIf(PlayingAfterimage::done);
        if (COPIES.isEmpty()) {
            return;
        }
        double now = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        COPIES.removeIf(copy -> {
            if (now - copy.born >= copy.owner.effect.lifetime()) {
                copy.owner.copies--;
                return true;
            }
            return false;
        });
        render(now, false);
    }

    public static void onHandsRendered(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !COPIES.isEmpty()) {
            render(level.getGameTime() + partialTick, true);
        }
    }

    private static void render(double now, boolean hands) {
        Map<RenderType, List<Copy>> batches = new LinkedHashMap<>();
        for (Copy copy : COPIES) {
            if (copy.owner.hand == hands) {
                batches.computeIfAbsent(type(copy), type -> new ArrayList<>()).add(copy);
            }
        }
        if (batches.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = ClientView.camera();
        for (Map.Entry<RenderType, List<Copy>> batch : batches.entrySet()) {
            VertexConsumer consumer = buffers.getBuffer(batch.getKey());
            for (Copy copy : batch.getValue()) {
                draw(copy, consumer, camera, copy.owner.effect.argb((float) (now - copy.born)));
            }
            buffers.endBatch(batch.getKey());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        COPIES.clear();
    }

    private static void draw(Copy copy, VertexConsumer consumer, Vec3 camera, int argb) {
        float[] v = copy.vertices;
        float dx = (float) (copy.origin.x - camera.x);
        float dy = (float) (copy.origin.y - camera.y);
        float dz = (float) (copy.origin.z - camera.z);
        for (int i = 0; i < v.length; i += Recorder.STRIDE) {
            float x = v[i] + dx;
            float y = v[i + 1] + dy;
            float z = v[i + 2] + dz;
            if (copy.texture == null) {
                consumer.addVertex(x, y, z).setColor(argb).setLight(copy.light);
            } else {
                consumer.addVertex(x, y, z, argb, v[i + 3], v[i + 4], OverlayTexture.NO_OVERLAY, copy.light, v[i + 5], v[i + 6], v[i + 7]);
            }
        }
    }

    private static RenderType type(Copy copy) {
        Blend blend = copy.owner.effect.blend();
        if (copy.texture == null) {
            return blend == Blend.ADDITIVE ? ADDITIVE : TRANSLUCENT;
        }
        return blend == Blend.ADDITIVE ? RenderType.eyes(copy.texture) : RenderType.entityTranslucent(copy.texture);
    }

    // No depth write, so the copies of one effect do not hide each other
    private static RenderType renderType(String name, Blend blend) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setTransparencyState(blend.transparency())
                        .createCompositeState(false));
    }

    // Vertices are relative to the camera position they were captured at, origin
    private record Copy(PlayingAfterimage owner, float[] vertices, Vec3 origin, @Nullable ResourceLocation texture, int light, double born) {
    }

    private static final class Recorder implements VertexConsumer {
        static final int STRIDE = 8;

        private float[] vertices = new float[STRIDE * 256];
        private int size;

        void start() {
            size = 0;
        }

        float[] finish() {
            float[] copy = new float[size];
            System.arraycopy(vertices, 0, copy, 0, size);
            return copy;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (size + STRIDE > vertices.length) {
                float[] grown = new float[vertices.length * 2];
                System.arraycopy(vertices, 0, grown, 0, size);
                vertices = grown;
            }
            vertices[size] = x;
            vertices[size + 1] = y;
            vertices[size + 2] = z;
            size += STRIDE;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            vertices[size - STRIDE + 3] = u;
            vertices[size - STRIDE + 4] = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            vertices[size - STRIDE + 5] = x;
            vertices[size - STRIDE + 6] = y;
            vertices[size - STRIDE + 7] = z;
            return this;
        }
    }
}
