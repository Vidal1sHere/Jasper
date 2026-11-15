package com.vidal1shere.jasper.client.effect;

import com.vidal1shere.jasper.client.ClientAttachments;
import com.vidal1shere.jasper.client.attachment.LocatorData;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class EffectContext {
    private static final long FRESH_TICKS = 2L;

    private final Entity entity;
    private final AnimationTarget target;
    @Nullable
    private final String locator;
    private final boolean follow;
    private final boolean persist;
    private final boolean bound;
    private Vec3 position;

    public EffectContext(Entity entity, AnimationTarget target, @Nullable String locator, boolean follow, boolean persist, boolean bound) {
        this.entity = entity;
        this.target = target;
        this.locator = locator;
        this.follow = follow;
        this.persist = persist;
        this.bound = bound;
        Vec3 located = located();
        this.position = located != null ? located : entity.position();
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

    // An until of its entry decides when it ends, so an effect with a duration of its own holds until then instead
    public boolean bound() {
        return bound;
    }

    // A following effect falls back to the entity while its locator is not rendered (first person, off screen)
    public Vec3 position() {
        if (follow && !entity.isRemoved()) {
            Vec3 located = located();
            position = located != null ? located : entity.position();
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
