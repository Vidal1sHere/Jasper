package com.vidal1shere.jasper.client.attachment;

import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CapturedLocator(Vec3 position, long tick, long frame) {
}
