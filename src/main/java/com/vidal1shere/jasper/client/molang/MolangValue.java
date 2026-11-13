package com.vidal1shere.jasper.client.molang;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@FunctionalInterface
public interface MolangValue {
    MolangValue ZERO = constant(0.0F);

    float get(MolangContext context);

    default boolean isConstant() {
        return false;
    }

    static MolangValue constant(float value) {
        return new Constant(value);
    }

    @OnlyIn(Dist.CLIENT)
    record Constant(float value) implements MolangValue {
        @Override
        public float get(MolangContext context) {
            return value;
        }

        @Override
        public boolean isConstant() {
            return true;
        }
    }
}
