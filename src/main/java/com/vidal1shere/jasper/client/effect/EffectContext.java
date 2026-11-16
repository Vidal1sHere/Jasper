package com.vidal1shere.jasper.client.effect;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.attachment.LocatorData;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class EffectContext {
    public static final long FRESH_TICKS = 2L;

    private final Entity entity;
    private final AnimationTarget target;
    @Nullable
    private final String locator;
    private final boolean follow;
    private final boolean persist;
    private final boolean bound;
    private final Supplier<Vec3> fallback;
    private Vec3 position;

    public EffectContext(Entity entity, AnimationTarget target, @Nullable String locator, boolean follow, boolean persist, boolean bound) {
        this(entity, target, locator, follow, persist, bound, entity::position);
    }

    // The fallback stands in while the locator is not rendered, or always without one
    public EffectContext(Entity entity, AnimationTarget target, @Nullable String locator, boolean follow, boolean persist, boolean bound, Supplier<Vec3> fallback) {
        this.entity = entity;
        this.target = target;
        this.locator = locator;
        this.follow = follow;
        this.persist = persist;
        this.bound = bound;
        this.fallback = fallback;
        Vec3 located = located();
        this.position = located != null ? located : fallback.get();
    }

    public Entity entity() {
        return entity;
    }

    public AnimationTarget target() {
        return target;
    }

    public boolean follow() {
        return follow;
    }

    public boolean persist() {
        return persist;
    }

    public boolean bound() {
        return bound;
    }

    // A following effect falls back to the entity while its locator is not rendered (first person, off screen)
    public Vec3 position() {
        if (follow && !entity.isRemoved()) {
            Vec3 located = located();
            position = located != null ? located : fallback.get();
        }
        return position;
    }

    @Nullable
    private Vec3 located() {
        if (locator == null) {
            return null;
        }
        LocatorData data = entity.getExistingDataOrNull(ClientAttachments.LOCATORS);
        return data == null ? null : data.get(target, locator, entity.level().getGameTime() - FRESH_TICKS);
    }
}
