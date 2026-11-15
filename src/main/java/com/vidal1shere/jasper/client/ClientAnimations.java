package com.vidal1shere.jasper.client;

import com.vidal1shere.jasper.client.attachment.LayerState;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ClientAnimations {
    public static void play(Entity entity, Layer layer, AnimationTarget target, ResourceLocation animation, PlayOptions options) {
        play(entity, layer, target, animation, entity.level().getGameTime(), options);
    }

    public static void play(Entity entity, Layer layer, AnimationTarget target, @Nullable ResourceLocation animation, long startTick, PlayOptions options) {
        LayerState track = entity.getData(ClientAttachments.ANIMATIONS).track(target, layer, loader(entity, target));
        if (animation != null) {
            track.setHand(options.hand());
        }
        track.play(animation, startTick, options);
    }

    public static void receive(int entityId, Layer layer, AnimationTarget target, @Nullable ResourceLocation animation, long startTick, PlayOptions options) {
        Entity entity = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            play(entity, layer, target, animation, startTick, options);
        }
    }

    public static void stop(Entity entity, Layer layer, AnimationTarget target) {
        play(entity, layer, target, null, entity.level().getGameTime(), PlayOptions.DEFAULT);
    }

    private static JasperAnimationLoader loader(Entity entity, AnimationTarget target) {
        if (target != AnimationTarget.MODEL) {
            return JasperAnimationLoader.ITEM;
        }
        return entity instanceof Player ? JasperAnimationLoader.PLAYER : JasperAnimationLoader.ENTITY;
    }
}
