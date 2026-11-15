package com.vidal1shere.jasper.client.effect.trail;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.animation.locator.LocatorCapture;
import com.vidal1shere.jasper.client.effect.EffectContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/*
 * Rings are recorded where this frame's locators are known and drawn in camera relative world space: model trails after the entities and the particles,
 * hand trails at the end of the hand pass, with its projection and its depth, so they line up with the item
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class Trails {
    private static final RenderType ADDITIVE = renderType("jasper_trail_additive", RenderStateShard.LIGHTNING_TRANSPARENCY, null);
    private static final RenderType TRANSLUCENT = renderType("jasper_trail_translucent", RenderStateShard.TRANSLUCENT_TRANSPARENCY, null);
    private static final BiFunction<ResourceLocation, TrailEffect.Blend, RenderType> TEXTURED = Util.memoize((texture, blend) -> renderType(
            "jasper_trail_" + blend.getSerializedName() + "_" + texture.toDebugFileName(), transparency(blend), texture));

    private static final List<PlayingTrail> ACTIVE = new ArrayList<>();

    static PlayingTrail start(TrailEffect effect, EffectContext context) {
        PlayingTrail trail = new PlayingTrail(effect, context);
        ACTIVE.add(trail);
        return trail;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (ACTIVE.isEmpty() || level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = level.getGameTime() + partialTick;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            long frame = LocatorCapture.renderFrame();
            for (PlayingTrail trail : ACTIVE) {
                if (!trail.hand()) {
                    trail.record(frame, now, partialTick);
                }
            }
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ACTIVE.removeIf(trail -> {
                trail.trim(now);
                return trail.finished();
            });
            render(level, now, partialTick, false);
        }
    }

    public static void onHandsRendered(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (ACTIVE.isEmpty() || level == null) {
            return;
        }
        double now = level.getGameTime() + partialTick;
        long frame = LocatorCapture.renderFrame();
        for (PlayingTrail trail : ACTIVE) {
            if (trail.hand()) {
                trail.record(frame, now, partialTick);
            }
        }
        render(level, now, partialTick, true);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    private static void render(ClientLevel level, double now, float partialTick, boolean hands) {
        Map<RenderType, List<PlayingTrail>> batches = new LinkedHashMap<>();
        for (PlayingTrail trail : ACTIVE) {
            if (trail.hand() == hands && trail.rings.size() >= 2) {
                batches.computeIfAbsent(type(trail.effect), type -> new ArrayList<>()).add(trail);
            }
        }
        if (batches.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (Map.Entry<RenderType, List<PlayingTrail>> batch : batches.entrySet()) {
            VertexConsumer consumer = buffers.getBuffer(batch.getKey());
            boolean textured = batch.getKey() != ADDITIVE && batch.getKey() != TRANSLUCENT;
            for (PlayingTrail trail : batch.getValue()) {
                mesh(trail, consumer, textured, level, camera, now, partialTick);
            }
            buffers.endBatch(batch.getKey());
        }
    }

    // Quads between two consecutive samples, joining point i to point i + 1 of the section
    private static void mesh(PlayingTrail trail, VertexConsumer consumer, boolean textured, ClientLevel level, Vec3 camera, double now, float partialTick) {
        TrailEffect effect = trail.effect;
        List<Sample> samples = samples(trail, trail.origin(partialTick), now);
        if (effect.locators().size() == 1) {
            samples = widen(samples, camera, effect.width() / 2.0F);
        }
        for (int k = 1; k < samples.size(); k++) {
            Sample previous = samples.get(k - 1);
            Sample sample = samples.get(k);
            for (int i = 0; i < sample.points().length - 1; i++) {
                vertex(consumer, textured, level, camera, effect, previous, i);
                vertex(consumer, textured, level, camera, effect, previous, i + 1);
                vertex(consumer, textured, level, camera, effect, sample, i + 1);
                vertex(consumer, textured, level, camera, effect, sample, i);
            }
        }
    }

    // The rings in world space, with smoothing points between each two on a Catmull-Rom curve through the rings around them
    private static List<Sample> samples(PlayingTrail trail, Vec3 origin, double now) {
        List<PlayingTrail.Ring> rings = new ArrayList<>(trail.rings);
        int smoothing = trail.effect.smoothing();
        float lifetime = trail.effect.lifetime();
        List<Sample> samples = new ArrayList<>(rings.size() * (smoothing + 1));
        int count = trail.effect.locators().size();
        for (int k = 0; k < rings.size(); k++) {
            PlayingTrail.Ring ring = rings.get(k);
            float age = (float) ((now - ring.time()) / lifetime);
            samples.add(new Sample(world(ring.points(), origin), age));
            if (k == rings.size() - 1) {
                break;
            }
            PlayingTrail.Ring next = rings.get(k + 1);
            float nextAge = (float) ((now - next.time()) / lifetime);
            for (int s = 1; s <= smoothing; s++) {
                float t = s / (smoothing + 1.0F);
                Vec3[] points = new Vec3[count];
                for (int i = 0; i < count; i++) {
                    Vec3 p1 = ring.points()[i];
                    Vec3 p2 = next.points()[i];
                    Vec3 p0 = k > 0 ? rings.get(k - 1).points()[i] : p1.scale(2.0).subtract(p2);
                    Vec3 p3 = k + 2 < rings.size() ? rings.get(k + 2).points()[i] : p2.scale(2.0).subtract(p1);
                    points[i] = catmullRom(p0, p1, p2, p3, t).add(origin);
                }
                samples.add(new Sample(points, age + (nextAge - age) * t));
            }
        }
        return samples;
    }

    // A one point trail becomes a section of two points across its path, facing the camera
    private static List<Sample> widen(List<Sample> samples, Vec3 camera, float half) {
        List<Sample> wide = new ArrayList<>(samples.size());
        Vec3 side = new Vec3(0.0, 1.0, 0.0);
        for (int k = 0; k < samples.size(); k++) {
            Vec3 point = samples.get(k).points()[0];
            Vec3 tangent = samples.get(Math.min(k + 1, samples.size() - 1)).points()[0].subtract(samples.get(Math.max(k - 1, 0)).points()[0]);
            Vec3 across = tangent.cross(point.subtract(camera));
            if (across.lengthSqr() > 1.0E-10) {
                side = across.normalize();
            }
            wide.add(new Sample(new Vec3[]{point.subtract(side.scale(half)), point.add(side.scale(half))}, samples.get(k).age()));
        }
        return wide;
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3(
                catmullRom(p0.x, p1.x, p2.x, p3.x, t, t2, t3),
                catmullRom(p0.y, p1.y, p2.y, p3.y, t, t2, t3),
                catmullRom(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static double catmullRom(double p0, double p1, double p2, double p3, double t, double t2, double t3) {
        return 0.5 * (2.0 * p1 + (p2 - p0) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (3.0 * p1 - p0 - 3.0 * p2 + p3) * t3);
    }

    private static Vec3[] world(Vec3[] points, Vec3 origin) {
        Vec3[] world = new Vec3[points.length];
        for (int i = 0; i < points.length; i++) {
            world[i] = points[i].add(origin);
        }
        return world;
    }

    // U goes along the length with the age, V across the section
    private static void vertex(VertexConsumer consumer, boolean textured, ClientLevel level, Vec3 camera, TrailEffect effect, Sample sample, int index) {
        Vec3 point = sample.points()[index];
        float shrink = effect.shrinking(sample.age());
        if (shrink > 0.0F) {
            point = point.lerp(center(sample.points()), shrink);
        }
        int light = effect.light() == TrailEffect.Light.FULL ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, BlockPos.containing(point));
        consumer.addVertex((float) (point.x - camera.x), (float) (point.y - camera.y), (float) (point.z - camera.z))
                .setColor(effect.argb(sample.age(), index));
        if (textured) {
            consumer.setUv(sample.age(), index / (sample.points().length - 1.0F));
        }
        consumer.setLight(light);
    }

    private static Vec3 center(Vec3[] points) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (Vec3 point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        return new Vec3(x / points.length, y / points.length, z / points.length);
    }

    private static RenderType type(TrailEffect effect) {
        if (effect.texture().isPresent()) {
            return TEXTURED.apply(effect.texture().get(), effect.blend());
        }
        return effect.blend() == TrailEffect.Blend.ADDITIVE ? ADDITIVE : TRANSLUCENT;
    }

    private static RenderStateShard.TransparencyStateShard transparency(TrailEffect.Blend blend) {
        return blend == TrailEffect.Blend.ADDITIVE ? RenderStateShard.LIGHTNING_TRANSPARENCY : RenderStateShard.TRANSLUCENT_TRANSPARENCY;
    }

    // No culling, so the ribbon shows from both sides, and no depth write, so trails do not hide each other
    private static RenderType renderType(String name, RenderStateShard.TransparencyStateShard transparency, ResourceLocation texture) {
        RenderType.CompositeState.CompositeStateBuilder state = RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setTransparencyState(transparency);
        if (texture == null) {
            state.setShaderState(RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER);
            return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, true, state.createCompositeState(false));
        }
        state.setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false));
        return RenderType.create(name, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1536, false, true, state.createCompositeState(false));
    }

    private record Sample(Vec3[] points, float age) {
    }
}
