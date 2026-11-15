package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.layer.LayerKey;

public record BoundEffect(LayerKey key, AnimationTrack.Entry entry, PlayingEffect instance, EffectContext context, float until, boolean persist) {
}
