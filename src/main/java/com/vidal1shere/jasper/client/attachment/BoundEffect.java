package com.vidal1shere.jasper.client.attachment;

import com.vidal1shere.jasper.client.animation.AnimationTrack;
import com.vidal1shere.jasper.client.effect.EffectContext;
import com.vidal1shere.jasper.client.effect.PlayingEffect;
import com.vidal1shere.jasper.layer.LayerKey;

// Until is the elapsed time of the entry at which it ends, or a constant of EffectEntry
public record BoundEffect(LayerKey key, AnimationTrack.Entry entry, PlayingEffect instance, EffectContext context, float until, boolean persist) {
}
