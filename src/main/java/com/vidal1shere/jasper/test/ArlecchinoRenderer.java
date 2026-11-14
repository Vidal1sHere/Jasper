package com.vidal1shere.jasper.test;

import com.vidal1shere.jasper.Jasper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ArlecchinoRenderer extends MobRenderer<Arlecchino, ArlecchinoModel> {
    private static final ResourceLocation TEXTURE = Jasper.id("textures/entity/arlecchino.png");

    public ArlecchinoRenderer(EntityRendererProvider.Context context) {
        super(context, new ArlecchinoModel(context.bakeLayer(ArlecchinoModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Arlecchino arlecchino) {
        return TEXTURE;
    }
}
