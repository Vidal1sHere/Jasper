package com.vidal1shere.jasper.client.item;

import com.google.gson.JsonObject;
import com.vidal1shere.jasper.client.keyframe.serialization.KeyframeAnimationLoader;
import com.vidal1shere.jasper.client.keyframe.serialization.KeyframeJson;
import com.vidal1shere.jasper.Jasper;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Vector3f;

import java.util.Set;

@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class ItemAnimationLoader extends KeyframeAnimationLoader<ItemAnimation> {
    public static final ItemAnimationLoader INSTANCE = new ItemAnimationLoader();

    private static final float PIXEL = 1.0F / 16.0F;
    private static final KeyframeJson.Units UNITS = new KeyframeJson.Units(
            new Vector3f(PIXEL, -PIXEL, PIXEL),
            new Vector3f(Mth.DEG_TO_RAD)
    );

    private ItemAnimationLoader() {
        super("jasper/animation/item", "item");
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    @Override
    protected ItemAnimation parse(JsonObject json) {
        Vector3f pivot = json.has("pivot") ? KeyframeJson.constantVector(json.get("pivot"), "pivot").mul(PIXEL) : ItemAnimation.NO_PIVOT;
        return new ItemAnimation(KeyframeJson.parse(json, Set.of(ItemAnimation.BONE), UNITS, Set.of("pivot")), pivot);
    }
}
