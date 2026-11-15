package com.vidal1shere.jasper.layer;

import java.util.Set;

public record LayerPolicy(Set<EndCondition> endsOn, boolean persistent) {
    public LayerPolicy {
        endsOn = Set.copyOf(endsOn);
    }

    public boolean endsOn(EndCondition condition) {
        return endsOn.contains(condition);
    }
}
