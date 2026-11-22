package com.vidal1shere.jasper.client.item.attachment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/*
 * jasper:player_arm, the arm and sleeve of the player model of the holder, with its skin, first person only (in third person the
 * player model has its own arm). The shoulder pivot sits at the attachment; without a player holder, the default wide skin.
 */
@OnlyIn(Dist.CLIENT)
public class PlayerArmAttachment extends JasperModelAttachment {
    public static final MapCodec<PlayerArmAttachment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttachmentPlacement.CODEC.forGetter(JasperModelAttachment::placement),
            HumanoidArm.CODEC.fieldOf("arm").forGetter(PlayerArmAttachment::arm)
    ).apply(instance, PlayerArmAttachment::new));

    private static final ModelPart SAVED_ARM = new ModelPart(List.of(), Map.of());
    private static final ModelPart SAVED_SLEEVE = new ModelPart(List.of(), Map.of());

    private final HumanoidArm arm;

    public PlayerArmAttachment(AttachmentPlacement placement, HumanoidArm arm) {
        super(placement);
        this.arm = arm;
    }

    public HumanoidArm arm() {
        return arm;
    }

    @Override
    public void render(AttachmentContext context, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!context.displayContext().firstPerson()) {
            return;
        }
        AbstractClientPlayer player = context.holder() instanceof AbstractClientPlayer holder ? holder : null;
        ResourceLocation texture = texture(context);
        PlayerModel<AbstractClientPlayer> model = model(player);
        if (model == null) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        ModelPart armPart = left ? model.leftArm : model.rightArm;
        ModelPart sleevePart = left ? model.leftSleeve : model.rightSleeve;
        boolean sleeve = player == null || player.isModelPartShown(left ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE);
        // The parts are shared by every player of that renderer, so their pose is put back
        SAVED_ARM.copyFrom(armPart);
        SAVED_SLEEVE.copyFrom(sleevePart);
        boolean armVisible = armPart.visible;
        boolean sleeveVisible = sleevePart.visible;
        armPart.loadPose(PartPose.ZERO);
        sleevePart.loadPose(PartPose.ZERO);
        armPart.visible = true;
        sleevePart.visible = sleeve;
        poseStack.pushPose();
        // Back to model space, Y down, where the arm hangs from its shoulder pivot
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        armPart.render(poseStack, buffer.getBuffer(RenderType.entitySolid(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        sleevePart.render(poseStack, buffer.getBuffer(RenderType.entityTranslucent(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        armPart.copyFrom(SAVED_ARM);
        sleevePart.copyFrom(SAVED_SLEEVE);
        armPart.visible = armVisible;
        sleevePart.visible = sleeveVisible;
    }

    @Override
    public ResourceLocation texture(AttachmentContext context) {
        return context.holder() instanceof AbstractClientPlayer player ? player.getSkin().texture() : DefaultPlayerSkin.getDefaultTexture();
    }

    @Nullable
    private static PlayerModel<AbstractClientPlayer> model(@Nullable AbstractClientPlayer player) {
        EntityRenderer<?> renderer = player != null ? Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) : Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().get(PlayerSkin.Model.WIDE);
        return renderer instanceof PlayerRenderer playerRenderer ? playerRenderer.getModel() : null;
    }
}
