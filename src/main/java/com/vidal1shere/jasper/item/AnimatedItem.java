package com.vidal1shere.jasper.item;

import com.vidal1shere.jasper.ServerAnimations;
import com.vidal1shere.jasper.client.ClientAnimations;
import com.vidal1shere.jasper.layer.AnimationTarget;
import com.vidal1shere.jasper.layer.JasperLayers;
import com.vidal1shere.jasper.layer.Layer;
import com.vidal1shere.jasper.layer.PlayOptions;
import com.vidal1shere.jasper.layer.PlayView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface AnimatedItem {
    @Nullable
    default ResourceLocation texture(ItemStack stack, @Nullable LivingEntity holder, @Nullable InteractionHand hand) {
        return null;
    }

    @Nullable
    default String modelLayer(ItemStack stack, @Nullable LivingEntity holder, @Nullable InteractionHand hand) {
        return null;
    }

    default void setAnimation(LivingEntity holder, InteractionHand hand, ResourceLocation animation) {
        setAnimation(holder, hand, JasperLayers.BASE, animation);
    }

    default void setAnimation(LivingEntity holder, InteractionHand hand, Layer layer, ResourceLocation animation) {
        play(holder, hand, layer, animation, PlayOptions.DEFAULT);
    }

    default void setAnimation(LivingEntity holder, InteractionHand hand, ResourceLocation animation, ResourceLocation firstPerson) {
        setAnimation(holder, hand, JasperLayers.BASE, animation, firstPerson);
    }

    default void setAnimation(LivingEntity holder, InteractionHand hand, Layer layer, ResourceLocation animation, ResourceLocation firstPerson) {
        play(holder, hand, layer, animation, PlayOptions.DEFAULT.withFirstPerson(firstPerson));
    }

    default void setFirstPersonAnimation(LivingEntity holder, InteractionHand hand, ResourceLocation animation) {
        setFirstPersonAnimation(holder, hand, JasperLayers.BASE, animation);
    }

    default void setFirstPersonAnimation(LivingEntity holder, InteractionHand hand, Layer layer, ResourceLocation animation) {
        play(holder, hand, layer, animation, PlayOptions.DEFAULT.withView(PlayView.FIRST_PERSON));
    }

    default void setThirdPersonAnimation(LivingEntity holder, InteractionHand hand, ResourceLocation animation) {
        setThirdPersonAnimation(holder, hand, JasperLayers.BASE, animation);
    }

    default void setThirdPersonAnimation(LivingEntity holder, InteractionHand hand, Layer layer, ResourceLocation animation) {
        play(holder, hand, layer, animation, PlayOptions.DEFAULT.withView(PlayView.THIRD_PERSON));
    }

    private void play(LivingEntity holder, InteractionHand hand, Layer layer, ResourceLocation animation, PlayOptions options) {
        if (holder.level().isClientSide()) {
            ClientAnimations.play(holder, layer, AnimationTarget.of(hand), animation, options.withHand(hand));
        } else {
            ServerAnimations.play(holder, layer, AnimationTarget.of(hand), animation, options.withHand(hand));
        }
    }

    default void stopAnimation(LivingEntity holder, InteractionHand hand) {
        stopAnimation(holder, hand, JasperLayers.BASE);
    }

    default void stopAnimation(LivingEntity holder, InteractionHand hand, Layer layer) {
        if (holder.level().isClientSide()) {
            ClientAnimations.stop(holder, layer, AnimationTarget.of(hand));
        } else {
            ServerAnimations.stop(holder, layer, AnimationTarget.of(hand));
        }
    }

    @Nullable
    default ItemAnimationDriver driver(Layer layer) {
        return null;
    }
}
