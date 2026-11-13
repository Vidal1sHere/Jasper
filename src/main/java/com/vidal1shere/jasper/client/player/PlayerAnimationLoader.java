package com.vidal1shere.jasper.client.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.vidal1shere.jasper.client.keyframe.KeyframeAnimation;
import com.vidal1shere.jasper.client.keyframe.serialization.KeyframeAnimationLoader;
import com.vidal1shere.jasper.client.keyframe.serialization.KeyframeJson;
import com.vidal1shere.jasper.Jasper;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Jasper.MOD_ID, value = Dist.CLIENT)
public final class PlayerAnimationLoader extends KeyframeAnimationLoader<PlayerAnimation> {
    public static final PlayerAnimationLoader INSTANCE = new PlayerAnimationLoader("jasper/animation/player", "player", PlayerBones.ALL);
    public static final PlayerAnimationLoader ARM_POSES = new PlayerAnimationLoader("jasper/animation/arm_pose", "arm pose", Set.of(PlayerBones.TORSO, PlayerBones.HEAD, PlayerBones.RIGHT_ARM, PlayerBones.LEFT_ARM, PlayerBones.RIGHT_ITEM, PlayerBones.LEFT_ITEM, PlayerBones.RIGHT_LEG, PlayerBones.LEFT_LEG));

    private static final KeyframeJson.Units UNITS = new KeyframeJson.Units(
            new Vector3f(1.0F, -1.0F, 1.0F),
            new Vector3f(Mth.DEG_TO_RAD)
    );

    private final Set<String> bones;

    private PlayerAnimationLoader(String directory, String kind, Set<String> bones) {
        super(directory, kind);
        this.bones = bones;
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
        event.registerReloadListener(ARM_POSES);
    }

    @Override
    protected PlayerAnimation parse(JsonObject json) {
        PlayerAnimation.BlendMode mode = json.has("blend_mode") ? mode(GsonHelper.getAsString(json, "blend_mode"), "blend_mode") : PlayerAnimation.BlendMode.REPLACE;
        Map<String, PlayerAnimation.BlendMode> boneModes = new HashMap<>();
        if (json.has("bone_modes")) {
            for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(json, "bone_modes").entrySet()) {
                String field = "bone_modes." + entry.getKey();
                if (!bones.contains(entry.getKey())) {
                    throw new JsonParseException(field + ": unknown bone, expected one of " + bones);
                }
                boneModes.put(entry.getKey(), mode(GsonHelper.convertToString(entry.getValue(), field), field));
            }
        }
        KeyframeAnimation animation = KeyframeJson.parse(json, bones, UNITS, Set.of("blend_mode", "bone_modes"));
        return new PlayerAnimation(animation, mode, Map.copyOf(boneModes));
    }

    private static PlayerAnimation.BlendMode mode(String name, String field) {
        PlayerAnimation.BlendMode mode = PlayerAnimation.BlendMode.byName(name);
        if (mode == null) {
            throw new JsonParseException(field + ": expected 'replace' or 'additive', got '" + name + "'");
        }
        return mode;
    }
}
