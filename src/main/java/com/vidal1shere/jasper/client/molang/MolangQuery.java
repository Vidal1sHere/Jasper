package com.vidal1shere.jasper.client.molang;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// Queries are resolved when an animation is parsed, so they must be registered before the first resource load
@OnlyIn(Dist.CLIENT)
public final class MolangQuery {
    private static final Map<String, MolangQuery> BY_NAME = new HashMap<>();

    public static final MolangQuery ANIM_TIME = register("anim_time");
    public static final MolangQuery GROUND_SPEED = register("ground_speed");
    public static final MolangQuery VERTICAL_SPEED = register("vertical_speed");
    public static final MolangQuery IS_ON_GROUND = register("is_on_ground");
    public static final MolangQuery IS_SPRINTING = register("is_sprinting");
    public static final MolangQuery IS_SNEAKING = register("is_sneaking");
    public static final MolangQuery IS_SWIMMING = register("is_swimming");
    public static final MolangQuery IS_RIDING = register("is_riding");
    public static final MolangQuery HEAD_X_ROTATION = register("head_x_rotation");
    public static final MolangQuery HEAD_Y_ROTATION = register("head_y_rotation");
    public static final MolangQuery HAND_SIDE = register("hand_side");
    public static final MolangQuery LOOK_X_ROTATION = register("look_x_rotation");
    public static final MolangQuery LOOK_Y_ROTATION = register("look_y_rotation");

    private final String id;
    private final int index;
    private final boolean list;

    private MolangQuery(String id, int index, boolean list) {
        this.id = id;
        this.index = index;
        this.list = list;
    }

    public static MolangQuery register(String id) {
        return register(id, false);
    }

    public static MolangQuery registerList(String id) {
        return register(id, true);
    }

    private static synchronized MolangQuery register(String id, boolean list) {
        if (BY_NAME.containsKey(id)) {
            throw new IllegalStateException("Molang query '" + id + "' is already registered");
        }
        MolangQuery query = new MolangQuery(id, BY_NAME.size(), list);
        BY_NAME.put(id, query);
        return query;
    }

    @Nullable
    public static synchronized MolangQuery byName(String name) {
        return BY_NAME.get(name);
    }

    public String id() {
        return id;
    }

    public int index() {
        return index;
    }

    public boolean isList() {
        return list;
    }
}
