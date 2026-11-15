package com.vidal1shere.jasper.client.effect.timeline;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public record EffectTimeline(List<EffectEntry> entries) {
    public static final EffectTimeline EMPTY = new EffectTimeline(List.of());

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    // A cycling animation fires each entry once per cycle
    public void fire(float from, float to, float length, boolean cycles, Fired fired) {
        if (to <= from) {
            return;
        }
        if (!cycles || length <= 0.0F) {
            for (EffectEntry entry : entries) {
                if (entry.time() > from && entry.time() <= to) {
                    fired.accept(entry, entry.time());
                }
            }
            return;
        }
        int last = (int) Math.floor(to / length);
        for (int cycle = Math.max(0, (int) Math.floor(from / length)); cycle <= last; cycle++) {
            for (EffectEntry entry : entries) {
                float at = cycle * length + entry.time();
                if (at > from && at <= to) {
                    fired.accept(entry, at);
                }
            }
        }
    }

    @FunctionalInterface
    public interface Fired {
        void accept(EffectEntry entry, float at);
    }
}
