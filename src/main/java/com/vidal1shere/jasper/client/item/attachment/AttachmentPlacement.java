package com.vidal1shere.jasper.client.item.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

// bone is a part of the model or another attachment; placed like a display transform, translation in pixels, rotation in degrees
@OnlyIn(Dist.CLIENT)
public record AttachmentPlacement(String name, String bone, Vector3f translation, Vector3f rotation, float scale) {
    public static final MapCodec<AttachmentPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(AttachmentPlacement::name),
            Codec.STRING.fieldOf("bone").forGetter(AttachmentPlacement::bone),
            ExtraCodecs.VECTOR3F.optionalFieldOf("translation", new Vector3f()).forGetter(AttachmentPlacement::translation),
            ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(AttachmentPlacement::rotation),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(AttachmentPlacement::scale)
    ).apply(instance, AttachmentPlacement::new));

    public static AttachmentPlacement of(String name, String bone) {
        return new AttachmentPlacement(name, bone, new Vector3f(), new Vector3f(), 1.0F);
    }

    public AttachmentPlacement withTranslation(float x, float y, float z) {
        return new AttachmentPlacement(name, bone, new Vector3f(x, y, z), rotation, scale);
    }

    public AttachmentPlacement withRotation(float x, float y, float z) {
        return new AttachmentPlacement(name, bone, translation, new Vector3f(x, y, z), scale);
    }

    public AttachmentPlacement withScale(float scale) {
        return new AttachmentPlacement(name, bone, translation, rotation, scale);
    }
}
