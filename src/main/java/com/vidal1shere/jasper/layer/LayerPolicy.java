package com.vidal1shere.jasper.layer;

import java.util.Set;

// A persistent layer is saved with the entity, kept through death and played again when it loads
public record LayerPolicy(Set<EndCondition> endsOn, boolean persistent) {
    public LayerPolicy {
        endsOn = Set.copyOf(endsOn);
    }

    public boolean endsOn(EndCondition condition) {
        return endsOn.contains(condition);
    }
}
