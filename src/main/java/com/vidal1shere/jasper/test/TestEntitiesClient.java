package com.vidal1shere.jasper.test;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.animation.keyframe.VanillaAnimations;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class TestEntitiesClient {
    // Built with the vanilla format to exercise VanillaAnimations
    private static final AnimationDefinition SPIN = AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("root", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 360.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.3F, 1.3F, 1.3F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(1.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM)))
            .build();

    public static void register(IEventBus modBus) {
        JasperAnimationLoader.ENTITY.register(Jasper.id("test/arlecchino/spin"), () -> VanillaAnimations.convert(SPIN));
        JasperAnimationLoader.ENTITY.register(Jasper.id("test/arlecchino/idle"), () -> VanillaAnimations.convert(ArlecchinoAnimations.IDLE));
        JasperAnimationLoader.ENTITY.register(Jasper.id("test/arlecchino/walk"), () -> VanillaAnimations.convert(ArlecchinoAnimations.WALK));
        JasperAnimationLoader.ENTITY.register(Jasper.id("test/arlecchino/dazed"), () -> VanillaAnimations.convert(ArlecchinoAnimations.DAZED));
        JasperAnimationLoader.ENTITY.register(Jasper.id("test/arlecchino/peakaboo"), () -> VanillaAnimations.convert(ArlecchinoAnimations.PEAKABOO));
        modBus.addListener(TestEntitiesClient::onRegisterLayers);
        modBus.addListener(TestEntitiesClient::onRegisterRenderers);
    }

    private static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ArlecchinoModel.LAYER_LOCATION, ArlecchinoModel::createBodyLayer);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TestEntities.ARLECCHINO.get(), ArlecchinoRenderer::new);
    }
}
