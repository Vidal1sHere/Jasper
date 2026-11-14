package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.loading.JasperAnimationLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class EntityAnimationData {
    private final AnimationTrack main = new AnimationTrack(JasperAnimationLoader.ENTITY);
    private final AnimationTrack overlay = new AnimationTrack(JasperAnimationLoader.ENTITY);
    @Nullable
    private ResourceLocation action;
    private long actionStart;
    private float actionSpeed = 1.0F;
    @Nullable
    private ResourceLocation shown;
    private long shownStart;
    private boolean showingAction;

    public AnimationTrack main() {
        return main;
    }

    public AnimationTrack overlay() {
        return overlay;
    }

    @Nullable
    public ResourceLocation action() {
        return action;
    }

    public long actionStart() {
        return actionStart;
    }

    public float actionSpeed() {
        return actionSpeed;
    }

    public void setAction(@Nullable ResourceLocation action, long start, float speed) {
        this.action = action;
        this.actionStart = start;
        this.actionSpeed = speed;
    }

    @Nullable
    public ResourceLocation shown() {
        return shown;
    }

    public long shownStart() {
        return shownStart;
    }

    public boolean showingAction() {
        return showingAction;
    }

    public void setShown(ResourceLocation shown, long start, boolean action) {
        this.shown = shown;
        this.shownStart = start;
        this.showingAction = action;
    }
}
