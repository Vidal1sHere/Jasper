package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.attachment.locator.CapturedLocator;
import com.vidal1shere.jasper.layer.AnimationTarget;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class LocatorData {
    private final Map<AnimationTarget, Map<String, CapturedLocator>> captured = new EnumMap<>(AnimationTarget.class);

    public void put(AnimationTarget target, String locator, Vec3 position, long tick, long frame) {
        captured.computeIfAbsent(target, key -> new HashMap<>()).put(locator, new CapturedLocator(position, tick, frame));
    }

    // Null when it was never captured or only before the given tick
    @Nullable
    public Vec3 get(AnimationTarget target, String locator, long since) {
        Map<String, CapturedLocator> locators = captured.get(target);
        CapturedLocator found = locators == null ? null : locators.get(locator);
        return found == null || found.tick() < since ? null : found.position();
    }

    @Nullable
    public Vec3 inFrame(AnimationTarget target, String locator, long frame) {
        Map<String, CapturedLocator> locators = captured.get(target);
        CapturedLocator found = locators == null ? null : locators.get(locator);
        return found == null || found.frame() != frame ? null : found.position();
    }
}
