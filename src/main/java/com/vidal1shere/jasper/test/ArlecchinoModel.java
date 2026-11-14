package com.vidal1shere.jasper.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vidal1shere.jasper.Jasper;
import com.vidal1shere.jasper.client.entity.AnimatedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

public class ArlecchinoModel extends AnimatedModel<Arlecchino> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Jasper.id("model_arlecchino"), "main");
	public final ModelPart root;
	public final ModelPart head;
	public final ModelPart hat;
	public final ModelPart hat_left_ear;
	public final ModelPart left_bell;
	public final ModelPart hat_right_ear;
	public final ModelPart right_bell;
	public final ModelPart left_ear;
	public final ModelPart right_ear;
	public final ModelPart coils;
	public final ModelPart coil_1;
	public final ModelPart coil_2;
	public final ModelPart coil_3;
	public final ModelPart expression;
	public final ModelPart left_expression;
	public final ModelPart left_neutral;
	public final ModelPart left_devious;
	public final ModelPart left_skeptical;
	public final ModelPart left_surprised;
	public final ModelPart left_dead;
	public final ModelPart right_expression;
	public final ModelPart right_neutral;
	public final ModelPart right_devious;
	public final ModelPart right_skeptical;
	public final ModelPart right_surprised;
	public final ModelPart right_dead;
	public final ModelPart body;
	public final ModelPart torso;
	public final ModelPart ruffle;
	public final ModelPart left_arm;
	public final ModelPart right_arm;
	public final ModelPart left_leg;
	public final ModelPart right_leg;

	public ArlecchinoModel(ModelPart root) {
		super(root);
		this.root = root.getChild("root");
		this.head = this.root.getChild("head");
		this.hat = this.head.getChild("hat");
		this.hat_left_ear = this.hat.getChild("hat_left_ear");
		this.left_bell = this.hat_left_ear.getChild("left_bell");
		this.hat_right_ear = this.hat.getChild("hat_right_ear");
		this.right_bell = this.hat_right_ear.getChild("right_bell");
		this.left_ear = this.head.getChild("left_ear");
		this.right_ear = this.head.getChild("right_ear");
		this.coils = this.head.getChild("coils");
		this.coil_1 = this.coils.getChild("coil_1");
		this.coil_2 = this.coil_1.getChild("coil_2");
		this.coil_3 = this.coil_2.getChild("coil_3");
		this.expression = this.head.getChild("expression");
		this.left_expression = this.expression.getChild("left_expression");
		this.left_neutral = this.left_expression.getChild("left_neutral");
		this.left_devious = this.left_expression.getChild("left_devious");
		this.left_skeptical = this.left_expression.getChild("left_skeptical");
		this.left_surprised = this.left_expression.getChild("left_surprised");
		this.left_dead = this.left_expression.getChild("left_dead");
		this.right_expression = this.expression.getChild("right_expression");
		this.right_neutral = this.right_expression.getChild("right_neutral");
		this.right_devious = this.right_expression.getChild("right_devious");
		this.right_skeptical = this.right_expression.getChild("right_skeptical");
		this.right_surprised = this.right_expression.getChild("right_surprised");
		this.right_dead = this.right_expression.getChild("right_dead");
		this.body = this.root.getChild("body");
		this.torso = this.body.getChild("torso");
		this.ruffle = this.torso.getChild("ruffle");
		this.left_arm = this.body.getChild("left_arm");
		this.right_arm = this.body.getChild("right_arm");
		this.left_leg = this.body.getChild("left_leg");
		this.right_leg = this.body.getChild("right_leg");
		for (ModelPart eye : new ModelPart[]{left_devious, left_skeptical, left_surprised, left_dead, right_devious, right_skeptical, right_surprised, right_dead}) {
			eye.visible = false;
		}
	}

	@Override
	public ModelPart head() {
		return head;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(31, 1).addBox(-2.0F, -4.0F, -6.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(49, 4).addBox(5.0F, -6.0F, -1.5F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(49, 4).mirror().addBox(-8.0F, -6.0F, -1.5F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -24.0F, 0.0F));

		PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(66, 0).addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition hat_left_ear = hat.addOrReplaceChild("hat_left_ear", CubeListBuilder.create(), PartPose.offset(2.5F, -6.0F, 0.0F));

		PartDefinition cube_r1 = hat_left_ear.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(100, 18).addBox(0.0F, -3.0F, -2.0F, 10.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition cube_r2 = hat_left_ear.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(68, 16).addBox(0.0F, -3.0F, -3.0F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0472F));

		PartDefinition left_bell = hat_left_ear.addOrReplaceChild("left_bell", CubeListBuilder.create(), PartPose.offset(8.0F, 0.0F, 0.0F));

		PartDefinition cube_r3 = left_bell.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(76, 40).addBox(7.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.025F)), PartPose.offsetAndRotation(-5.0F, -5.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		PartDefinition hat_right_ear = hat.addOrReplaceChild("hat_right_ear", CubeListBuilder.create(), PartPose.offset(-2.5F, -6.0F, 0.0F));

		PartDefinition cube_r4 = hat_right_ear.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(100, 30).mirror().addBox(-10.0F, -3.0F, -2.0F, 10.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, -9.0F, 0.0F, 0.0F, 0.0F, -0.7854F));

		PartDefinition cube_r5 = hat_right_ear.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(68, 28).mirror().addBox(-10.0F, -3.0F, -3.0F, 10.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0472F));

		PartDefinition right_bell = hat_right_ear.addOrReplaceChild("right_bell", CubeListBuilder.create(), PartPose.offset(-8.0F, 0.0F, 0.5F));

		PartDefinition cube_r6 = right_bell.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(76, 40).mirror().addBox(-11.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.025F)).mirror(false), PartPose.offsetAndRotation(5.0F, -5.0F, -0.5F, 0.0F, 0.0F, -0.7854F));

		PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(41, 5).addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -6.0F, 0.0F, 0.0F, 0.0F, -0.3491F));

		PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(53, 5).mirror().addBox(-0.5F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-5.5F, -6.0F, 0.0F, 0.0F, 0.0F, 0.3491F));

		PartDefinition coils = head.addOrReplaceChild("coils", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition coil_1 = coils.addOrReplaceChild("coil_1", CubeListBuilder.create().texOffs(23, 54).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

		PartDefinition coil_2 = coil_1.addOrReplaceChild("coil_2", CubeListBuilder.create().texOffs(23, 54).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition coil_3 = coil_2.addOrReplaceChild("coil_3", CubeListBuilder.create().texOffs(23, 54).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition expression = head.addOrReplaceChild("expression", CubeListBuilder.create(), PartPose.offset(0.0F, -4.5F, -4.001F));

		PartDefinition left_expression = expression.addOrReplaceChild("left_expression", CubeListBuilder.create(), PartPose.offset(3.5F, 0.0F, 0.0F));

		PartDefinition left_neutral = left_expression.addOrReplaceChild("left_neutral", CubeListBuilder.create().texOffs(0, 80).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_devious = left_expression.addOrReplaceChild("left_devious", CubeListBuilder.create().texOffs(0, 85).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_skeptical = left_expression.addOrReplaceChild("left_skeptical", CubeListBuilder.create().texOffs(0, 90).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_surprised = left_expression.addOrReplaceChild("left_surprised", CubeListBuilder.create().texOffs(0, 95).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_dead = left_expression.addOrReplaceChild("left_dead", CubeListBuilder.create().texOffs(0, 100).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_expression = expression.addOrReplaceChild("right_expression", CubeListBuilder.create(), PartPose.offset(-3.5F, 0.0F, 0.0F));

		PartDefinition right_neutral = right_expression.addOrReplaceChild("right_neutral", CubeListBuilder.create().texOffs(0, 80).mirror().addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_devious = right_expression.addOrReplaceChild("right_devious", CubeListBuilder.create().texOffs(0, 85).mirror().addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_skeptical = right_expression.addOrReplaceChild("right_skeptical", CubeListBuilder.create().texOffs(0, 90).mirror().addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_surprised = right_expression.addOrReplaceChild("right_surprised", CubeListBuilder.create().texOffs(0, 95).mirror().addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_dead = right_expression.addOrReplaceChild("right_dead", CubeListBuilder.create().texOffs(0, 100).mirror().addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(16, 16).addBox(-5.0F, -6.0F, -3.5F, 10.0F, 12.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(16, 35).addBox(-5.0F, -6.0F, -3.5F, 10.0F, 12.0F, 7.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -18.0F, 0.0F));

		PartDefinition ruffle = torso.addOrReplaceChild("ruffle", CubeListBuilder.create().texOffs(84, 72).mirror().addBox(-6.75F, -0.2F, -5.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(68, 72).addBox(2.75F, -0.2F, -5.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(84, 66).mirror().addBox(-6.75F, -0.2F, 1.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(68, 66).addBox(2.75F, -0.2F, 1.75F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(68, 60).addBox(-6.0F, -0.2F, -2.0F, 12.0F, 2.0F, 4.0F, new CubeDeformation(-0.2F))
		.texOffs(68, 48).addBox(-3.0F, -0.2F, -5.0F, 6.0F, 2.0F, 10.0F, new CubeDeformation(-0.2F)), PartPose.offset(0.0F, -6.6F, 0.0F));

		PartDefinition left_arm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(50, 19).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(50, 51).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(7.0F, -24.0F, 0.0F));

		PartDefinition right_arm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(50, 35).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(50, 67).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false), PartPose.offset(-7.0F, -24.0F, 0.0F));

		PartDefinition left_leg = body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 19).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 51).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)).mirror(false), PartPose.offset(2.0F, -12.0F, 0.0F));

		PartDefinition right_leg = body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 35).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 51).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-2.0F, -12.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}
}