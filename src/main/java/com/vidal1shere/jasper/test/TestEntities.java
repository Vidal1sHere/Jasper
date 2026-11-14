package com.vidal1shere.jasper.test;

import com.vidal1shere.jasper.Jasper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class TestEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Jasper.MOD_ID);

    public static final Supplier<EntityType<Arlecchino>> ARLECCHINO = ENTITY_TYPES.register("arlecchino", () ->
            EntityType.Builder.of(Arlecchino::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.79F)
                    .clientTrackingRange(64)
                    .build("arlecchino")
    );

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(TestEntities::onCreateAttributes);
    }

    private static void onCreateAttributes(EntityAttributeCreationEvent event) {
        event.put(ARLECCHINO.get(), Arlecchino.createAttributes().build());
    }
}
