package lucifertotem.client;

import lucifertotem.EntityLandMedusa;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/** Biped angel: long purple hair down the back, a robe skirt, a small golden halo and two flapping wings. 64x64 texture. */
public class ModelLandMedusa extends ModelBiped {
    private final ModelRenderer hairBack, skirt, wingR, wingL, halo;
    private float wing;

    public ModelLandMedusa() {
        super(0f, 0f, 64, 64);
        hairBack = new ModelRenderer(this, 0, 32);
        hairBack.addBox(-4f, -1f, 2.1f, 8, 12, 2);
        bipedBody.addChild(hairBack);

        skirt = new ModelRenderer(this, 24, 32);
        skirt.addBox(-4f, 9f, -2f, 8, 6, 4, 0.6f);
        bipedBody.addChild(skirt);

        wingR = new ModelRenderer(this, 0, 46);
        wingR.addBox(-12f, -2f, 0f, 12, 16, 1);
        wingR.setRotationPoint(-1.5f, 1.5f, 2.5f);
        bipedBody.addChild(wingR);
        wingL = new ModelRenderer(this, 0, 46);
        wingL.mirror = true;
        wingL.addBox(0f, -2f, 0f, 12, 16, 1);
        wingL.setRotationPoint(1.5f, 1.5f, 2.5f);
        bipedBody.addChild(wingL);

        halo = new ModelRenderer(this, 48, 32);
        halo.addBox(-3f, -11f, -3f, 6, 1, 1);
        halo.addBox(-3f, -11f, 2f, 6, 1, 1);
        halo.setTextureOffset(48, 34).addBox(-3f, -11f, -2f, 1, 1, 4);
        halo.setTextureOffset(48, 34).addBox(2f, -11f, -2f, 1, 1, 4);
        bipedHead.addChild(halo);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase e, float limbSwing, float amount, float pt) {
        super.setLivingAnimations(e, limbSwing, amount, pt);
        if (e instanceof EntityLandMedusa) {
            EntityLandMedusa m = (EntityLandMedusa) e;
            wing = m.prevWingPhase + (m.wingPhase - m.prevWingPhase) * pt;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float amount, float age, float headYaw, float headPitch, float scale, Entity e) {
        super.setRotationAngles(limbSwing, amount, age, headYaw, headPitch, scale, e);
        float flap = MathHelper.sin(wing) * 0.35f;
        wingR.rotateAngleY = 0.55f + flap;
        wingL.rotateAngleY = -0.55f - flap;
        wingR.rotateAngleZ = 0.15f + flap * 0.4f;
        wingL.rotateAngleZ = -0.15f - flap * 0.4f;
        hairBack.rotateAngleX = 0.06f + amount * 0.25f;
        skirt.rotateAngleX = 0f;
    }
}
