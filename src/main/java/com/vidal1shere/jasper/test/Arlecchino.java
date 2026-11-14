package com.vidal1shere.jasper.test;

import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.entity.AnimatedEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class Arlecchino extends PathfinderMob implements AnimatedEntity {
    private static final ResourceLocation IDLE = Jasper.id("test/arlecchino/idle");
    private static final ResourceLocation WALK = Jasper.id("test/arlecchino/walk");

    public Arlecchino(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    // Only so the look of the animations has something to follow
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public ResourceLocation idleAnimation() {
        return IDLE;
    }

    @Override
    public ResourceLocation walkAnimation() {
        return WALK;
    }
}
