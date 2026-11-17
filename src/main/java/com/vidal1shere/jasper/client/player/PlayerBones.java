package com.vidal1shere.jasper.client.player;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

// Bone names of the reference player .geo.json model; "body" is the root, the vanilla body part is "torso"
@OnlyIn(Dist.CLIENT)
public final class PlayerBones {
    public static final String ROOT = "body";
    public static final String TORSO = "torso";
    public static final String HEAD = "head";
    public static final String RIGHT_ARM = "right_arm";
    public static final String LEFT_ARM = "left_arm";
    public static final String RIGHT_ITEM = "right_item";
    public static final String LEFT_ITEM = "left_item";
    public static final String RIGHT_LEG = "right_leg";
    public static final String LEFT_LEG = "left_leg";

    public static final Set<String> ALL = Set.of(ROOT, TORSO, HEAD, RIGHT_ARM, LEFT_ARM, RIGHT_ITEM, LEFT_ITEM, RIGHT_LEG, LEFT_LEG);

    public static final String[][] PAIRS = {{RIGHT_ARM, LEFT_ARM}, {RIGHT_ITEM, LEFT_ITEM}, {RIGHT_LEG, LEFT_LEG}};
    // The bones that are model parts, each with its outer layer
    public static final List<String> PARTS = List.of(HEAD, TORSO, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG);

    public static final Vector3f ROOT_PIVOT = new Vector3f(0.0F, 12.0F, 0.0F);
    public static final Vector3f RIGHT_ITEM_PIVOT = new Vector3f(-1.0F, 10.0F, -2.0F);
    public static final Vector3f LEFT_ITEM_PIVOT = new Vector3f(1.0F, 10.0F, -2.0F);

    @Nullable
    public static ModelPart part(PlayerModel<?> model, String bone) {
        return switch (bone) {
            case HEAD -> model.head;
            case TORSO -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
            default -> null;
        };
    }

    @Nullable
    public static ModelPart overlay(PlayerModel<?> model, String bone) {
        return switch (bone) {
            case HEAD -> model.hat;
            case TORSO -> model.jacket;
            case RIGHT_ARM -> model.rightSleeve;
            case LEFT_ARM -> model.leftSleeve;
            case RIGHT_LEG -> model.rightPants;
            case LEFT_LEG -> model.leftPants;
            default -> null;
        };
    }

    public static String counterpart(String bone) {
        for (String[] pair : PAIRS) {
            if (pair[0].equals(bone)) {
                return pair[1];
            }
            if (pair[1].equals(bone)) {
                return pair[0];
            }
        }
        return bone;
    }
}
